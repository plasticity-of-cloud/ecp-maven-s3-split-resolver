# Concurrency and Storage Architecture

## K8s EmptyDir Implications

### What Happens with EmptyDir Metadata

**EmptyDir is pod-local and ephemeral:**
- Each pod gets its own isolated EmptyDir volume
- Metadata written to EmptyDir is **NOT shared** between pods
- When pod terminates, EmptyDir and all metadata is **deleted**

### Metadata Files Affected

Maven writes these tracking files to EmptyDir:
- `_remote.repositories` - Tracks which repository provided each artifact
- `*.lastUpdated` - Timestamps of failed download attempts
- `resolver-status.properties` - Resolution state tracking
- `*.sha1` / `*.md5` - Local checksums

### Impact on Concurrent Builds

**Scenario: 10 pods building simultaneously**

✅ **Artifacts (S3):** Shared across all pods
- Pod A downloads `commons-lang3-3.12.0.jar` → writes to S3
- Pod B needs same artifact → reads from S3 (cache hit)
- All pods benefit from shared artifact cache

❌ **Metadata (EmptyDir):** Isolated per pod
- Each pod maintains its own metadata
- No metadata sharing between concurrent builds
- Each pod may re-download artifacts if not in S3 yet

### Consequences

**First-time downloads:**
- Multiple pods may download the same artifact simultaneously from Maven Central
- Each pod writes to S3 independently (see S3 locking section below)
- Metadata stays local to each pod

**Subsequent builds:**
- Artifacts already in S3 are reused (fast)
- Metadata is rebuilt from scratch in each pod's EmptyDir
- No metadata corruption possible (isolated storage)

**After pod termination:**
- Artifacts persist in S3 (permanent cache)
- Metadata is lost (ephemeral, not needed)
- Next build starts with empty metadata, but cached artifacts

## S3 Locking and Concurrent Writes

### Does S3 Support Server-Side Locking?

**No.** S3 does **not** provide file locking or coordination primitives.

### S3 Consistency Model

**Strong read-after-write consistency (since Dec 2020):**
- PUT → immediate GET returns new object
- DELETE → immediate GET returns 404
- LIST → immediately reflects changes

**But no atomic operations:**
- No "write if not exists"
- No "compare-and-swap"
- No file locking

### Concurrent Write Scenarios

**Scenario: 10 pods download `commons-lang3-3.12.0.jar` simultaneously**

#### What Happens:
1. All 10 pods check S3 → artifact not found
2. All 10 pods download from Maven Central
3. All 10 pods write to S3 at same path

#### S3 Behavior:
- **Last writer wins** - S3 accepts all 10 writes
- Final object is whichever write completed last
- No corruption (each write is atomic)
- No partial writes (S3 guarantees atomicity per object)

#### Is This Safe?

✅ **Yes, for Maven artifacts:**
- Maven artifacts are **immutable** (same version = identical content)
- `commons-lang3-3.12.0.jar` from Maven Central is always the same bytes
- All 10 pods write identical data
- "Last writer wins" produces correct result (all writes are identical)

❌ **Would be unsafe for:**
- Mutable files (content changes over time)
- Append operations (S3 doesn't support)
- Metadata files with random I/O (hence EmptyDir)

### Optimization: Reduce Redundant Downloads

**Current behavior:**
- Multiple pods may download same artifact simultaneously
- Wasteful network usage from Maven Central

**Potential improvements:**
1. **Check-before-download with retry:**
   ```java
   if (!existsInS3(artifact)) {
       downloadFromMavenCentral(artifact);
       uploadToS3(artifact);
   } else {
       // Another pod already uploaded, use cached version
   }
   ```
   - Race condition still exists (multiple pods check simultaneously)
   - But reduces redundant downloads in practice

2. **Distributed locking (external):**
   - Use DynamoDB conditional writes for coordination
   - Use Redis/Memcached for distributed locks
   - Adds complexity and external dependencies

3. **Accept redundancy:**
   - Simplest approach (current implementation)
   - Maven Central can handle concurrent downloads
   - S3 can handle concurrent writes
   - Artifacts are immutable, so no corruption risk

## Recommendations

### For Production Use

**Current architecture is safe:**
- No corruption risk (immutable artifacts + S3 atomicity)
- No metadata conflicts (isolated EmptyDir per pod)
- Artifacts cached in S3 across all builds

**Accept redundant downloads:**
- First build of new dependency may download multiple times
- Subsequent builds use S3 cache (fast)
- Trade-off: simplicity vs. network efficiency

**Monitor S3 costs:**
- PUT requests cost money ($0.005 per 1,000 requests)
- 10 concurrent builds = 10x PUT requests for new artifacts
- Usually negligible compared to compute costs

### If Redundancy Becomes Problem

**Add check-before-download:**
```java
Path s3Path = artifactDir.resolve(relativePath);
if (Files.exists(s3Path)) {
    return; // Already cached, skip download
}
// Proceed with download and upload
```

**Or use build orchestration:**
- Serialize dependency resolution phase
- Parallelize compilation phase
- Ensures only one pod downloads each artifact

## Summary

| Aspect | Behavior | Safe? |
|--------|----------|-------|
| Artifact storage (S3) | Shared across all pods | ✅ Yes (immutable) |
| Metadata storage (EmptyDir) | Isolated per pod | ✅ Yes (no sharing) |
| Concurrent S3 writes | Last writer wins | ✅ Yes (identical content) |
| Redundant downloads | Multiple pods may download same artifact | ⚠️ Wasteful but safe |
| S3 locking | Not supported | ✅ Not needed (immutability) |

**Bottom line:** The architecture is safe for concurrent builds. Redundant downloads are a trade-off for simplicity.
