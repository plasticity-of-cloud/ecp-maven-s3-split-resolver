# Feature Request: Expose S3 Conditional Writes Through Filesystem Interface

**Target Repository:** https://github.com/awslabs/mountpoint-s3

---

**Copyright © 2026 Plasticity.Cloud**  
This document may be freely shared and referenced with attribution.

---

## Summary

Add support for S3 conditional writes (`If-None-Match`, `If-Match`) through the Mountpoint filesystem interface to prevent redundant uploads in concurrent write scenarios.

## Motivation

### Use Case: Maven Artifact Caching in Kubernetes

When running concurrent Maven builds in Kubernetes with S3-backed artifact storage:

1. **Current Behavior:**
   - 10 concurrent jobs download the same artifact from Maven Central
   - All 10 jobs attempt to upload to S3 simultaneously
   - 5-10 jobs complete redundant uploads of identical content
   - Wasted bandwidth: ~450MB for a 50MB artifact (9 redundant uploads)

2. **Desired Behavior:**
   - First job uploads successfully
   - Jobs 2-10 get "file exists" error and use existing artifact
   - Bandwidth savings: 90% reduction in redundant uploads

### Other Use Cases

- **Build artifact caching**: CI/CD pipelines with parallel jobs
- **ML model storage**: Multiple training jobs saving identical checkpoints
- **Container image layers**: Concurrent image builds with shared layers
- **Data processing**: Multiple workers writing deduplicated results

## Problem

S3 supports conditional writes via `If-None-Match` and `If-Match` headers (introduced November 2024), but Mountpoint doesn't expose these through the POSIX filesystem interface.

**Current Mountpoint behavior:**
- Last-writer-wins for concurrent writes
- No way to prevent overwrites
- No coordination between multiple Mountpoint instances

**Internal support exists:**
- `PutObjectSingleParams` has `if_match` field
- `RenameObjectParams` has both `if_match` and `if_none_match`
- Used internally for S3 Express One Zone append/rename operations
- Not exposed through filesystem operations

## Proposed Solution

### Option 1: Mount-time Flag (Recommended)

Add `--conditional-writes` flag that applies `If-None-Match: "*"` to all PutObject operations:

```bash
mount-s3 my-bucket /mnt/s3 --conditional-writes=if-none-match
```

**Behavior:**
- All file writes include `If-None-Match: "*"` header
- S3 returns `412 Precondition Failed` if object exists
- Mountpoint maps to `EEXIST` errno (File exists)
- Application sees standard POSIX error

**Application code:**
```java
try {
    Files.copy(source, s3Path);
} catch (FileAlreadyExistsException e) {
    // Artifact already uploaded, use existing
    logger.debug("File exists, skipping upload");
}
```

### Option 2: Extended Attributes (xattr)

Allow per-file control via extended attributes:

```bash
setfattr -n user.s3.if-none-match -v "*" /mnt/s3/artifact.jar
cp artifact.jar /mnt/s3/artifact.jar  # Returns EEXIST if exists
```

**Pros:** Fine-grained control per operation
**Cons:** Requires xattr support (currently not implemented)

### Option 3: Special Directory Prefix

Files under special directory use conditional writes:

```bash
cp artifact.jar /mnt/s3/.s3-conditional/artifact.jar
```

**Pros:** No API changes, works with existing tools
**Cons:** Awkward UX, path manipulation needed

## Implementation Details

### Changes Required

1. **CLI Arguments** (`mountpoint-s3/src/cli.rs`):
   ```rust
   #[arg(long)]
   conditional_writes: Option<ConditionalWriteMode>,
   
   enum ConditionalWriteMode {
       IfNoneMatch,
       // Future: IfMatch with ETag
   }
   ```

2. **Filesystem Layer** (`mountpoint-s3-fs/src/superblock.rs`):
   - Pass conditional write mode to write operations
   - Map S3 `412 Precondition Failed` → `EEXIST` errno

3. **S3 Client Layer** (`mountpoint-s3-client/src/s3_crt_client/put_object.rs`):
   - Already supports `if_match` in `PutObjectSingleParams`
   - Add `if_none_match` field (similar to `RenameObjectParams`)
   - Set header when mode is enabled

4. **Error Handling**:
   ```rust
   match error {
       S3Error::PreconditionFailed => Err(libc::EEXIST),
       _ => // existing error handling
   }
   ```

### Backward Compatibility

- Default behavior unchanged (no conditional writes)
- Opt-in via explicit flag
- No breaking changes to existing mounts

## Benefits

1. **Bandwidth Savings**: 90% reduction in redundant uploads for concurrent scenarios
2. **Cost Savings**: Fewer PUT requests to S3
3. **Faster Builds**: Jobs skip upload, use existing artifacts immediately
4. **Standard POSIX Semantics**: Applications use familiar error handling
5. **No Application Changes**: Works with existing tools (Maven, Gradle, npm, etc.)

## Alternatives Considered

### 1. Application-Level Coordination
- Use DynamoDB for locking
- **Rejected**: Adds complexity, cost, and latency

### 2. Direct S3 SDK Usage
- Bypass Mountpoint, use boto3/AWS SDK
- **Rejected**: Requires application changes, loses filesystem benefits

### 3. Pre-check Before Upload
- Check if object exists before writing
- **Rejected**: Race condition between check and write

## References

- [S3 Conditional Writes Documentation](https://docs.aws.amazon.com/AmazonS3/latest/userguide/conditional-writes.html)
- [S3 Conditional Writes Announcement](https://aws.amazon.com/blogs/storage/building-multi-writer-applications-on-amazon-s3-using-native-controls/)
- [Mountpoint Semantics Documentation](https://github.com/awslabs/mountpoint-s3/blob/main/doc/SEMANTICS.md)

## Related Code

Existing conditional write support in Mountpoint:

- `mountpoint-s3-client/src/object_client.rs`: `RenameObjectParams` with `if_match`/`if_none_match`
- `mountpoint-s3-client/src/s3_crt_client/put_object.rs`: `if_match` header support
- `mountpoint-s3-client/src/s3_crt_client/rename_object.rs`: Full conditional write implementation

## Questions for Maintainers

1. Is Option 1 (mount-time flag) the preferred approach?
2. Should we support both `If-None-Match` and `If-Match`, or start with just `If-None-Match`?
3. Any concerns about mapping `412 Precondition Failed` to `EEXIST`?
4. Would you accept a PR implementing this feature?

---

**Created:** 2026-03-17  
**Author:** Maven S3 Split Resolver Project  
**Status:** Draft - Not yet submitted to Mountpoint repository
