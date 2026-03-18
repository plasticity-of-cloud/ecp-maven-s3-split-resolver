# Maven S3 Split Resolver - Components

## Core Components

### S3SplitLocalRepositoryManager

**Location**: `src/main/java/cloud/plasticity/maven/resolver/S3SplitLocalRepositoryManager.java`

**Purpose**: Implements Maven's `LocalRepositoryManager` interface to route artifacts to S3 and metadata to local storage.

**Key Responsibilities**:
- Intercept artifact writes and route to S3
- Keep metadata files in local EmptyDir
- Use zero-copy transfer for efficient S3 uploads
- Create symlinks after transfer for fast artifact resolution

**Key Methods**:

| Method | Description |
|--------|-------------|
| `find(session, request)` | Check S3 for artifacts if not found in delegate |
| `add(session, registration)` | Copy artifact from EmptyDir to S3, then symlink |
| `zeroCopyFile(source, dest)` | Transfer file using FileChannel.transferTo() |
| `getPathForLocalArtifact(artifact)` | Get artifact path (delegates to delegate) |
| `getPathForRemoteArtifact(artifact, repo, context)` | Get remote artifact path (delegates) |
| `getPathForLocalMetadata(metadata)` | Get metadata path (delegates) |
| `getPathForRemoteMetadata(metadata, repo, context)` | Get remote metadata path (delegates) |
| `getRepository()` | Return metadata repository (EmptyDir) |

**State**:
- `delegate`: Original LocalRepositoryManager from Maven
- `artifactDir`: Path to S3 mount for artifacts
- `metadataDir`: Path to EmptyDir for metadata
- `localRepository`: LocalRepository instance for metadata

### S3SplitLocalRepositoryManagerFactory

**Location**: `src/main/java/cloud/plasticity/maven/resolver/S3SplitLocalRepositoryManagerFactory.java`

**Purpose**: Factory that creates the S3SplitLocalRepositoryManager when S3 integration is enabled.

**Key Responsibilities**:
- Implement Maven's `LocalRepositoryManagerFactory` SPI
- Check for `s3.resolver.artifactDir` system property
- Fall back to delegate if S3 not configured
- Set high priority (100.0) to override default manager

**Key Methods**:

| Method | Description |
|--------|-------------|
| `newInstance(session, repository)` | Create S3SplitLocalRepositoryManager if configured |
| `getPriority()` | Return priority (100.0) for SPI selection |

**Configuration**:
- Activates when: `System.getProperty("s3.resolver.artifactDir")` is set and non-empty
- Priority: 100.0 (higher than default)

## Extension Points

### Maven Resolver SPI

The extension integrates with Maven via two SPI interfaces:

1. **LocalRepositoryManagerFactory**
   - Maven calls this factory when creating a local repository manager
   - Priority determines which factory is selected
   - Returns null or throws exception to fall back to next factory

2. **LocalRepositoryManager**
   - Maven calls these methods for all local repository operations
   - Extension can intercept and customize behavior

## Configuration Components

### System Properties

| Property | Type | Required | Default | Description |
|----------|------|----------|---------|-------------|
| `s3.resolver.artifactDir` | String | No | None | Path to S3 mount for artifacts |
| `maven.repo.local` | String | No | ~/.m2/repository | Path to metadata storage |

### Maven Extension Configuration

```xml
<extensions>
  <extension>
    <groupId>cloud.plasticity</groupId>
    <artifactId>maven-s3-split-resolver</artifactId>
    <version>1.0.0</version>
  </extension>
</extensions>
```

## Storage Components

### S3 Mount (Artifacts)

**Mount Point**: Configured via `s3.resolver.artifactDir`

**Contents**:
- JAR files (`*.jar`)
- POM files (`*.pom`)
- Checksum files (`*.sha1`, `*.md5`)

**Characteristics**:
- Sequential write only (Mountpoint limitation)
- Immutable storage
- Shared across builds

### EmptyDir (Metadata)

**Mount Point**: Configured via `maven.repo.local`

**Contents**:
- `_remote.repositories` - Remote repository tracking
- `.lastUpdated` - Last update timestamps
- `resolver-status.properties` - Resolver state

**Characteristics**:
- Full POSIX I/O support
- Random read/write allowed
- Temporary storage (deleted on pod restart)

## Test Components

### S3SplitLocalRepositoryManagerTest

**Location**: `src/test/java/cloud/plasticity/maven/resolver/S3SplitLocalRepositoryManagerTest.java`

**Test Coverage**:
- `testGetRepository()` - Verify metadata repository
- `testGetPathForLocalArtifact()` - Verify artifact path generation
- `testFindArtifact_NotFound()` - Verify artifact not found handling
- `testFindArtifact_FoundInS3()` - Verify S3 artifact lookup
- `testAddArtifact()` - Verify artifact transfer to S3
- `testAddArtifact_NullFile()` - Verify null file handling
- `testFindMetadata_NotFound()` - Verify metadata delegation
- `testAddMetadata()` - Verify metadata delegation
- `testZeroCopyTransfer()` - Verify large file transfer
- `testPathSeparation()` - Verify storage separation

### S3SplitLocalRepositoryManagerFactoryTest

**Location**: `src/test/java/cloud/plasticity/maven/resolver/S3SplitLocalRepositoryManagerFactoryTest.java`

**Test Coverage**:
- `testGetPriority()` - Verify factory priority
- `testNewInstance_WithS3ArtifactDir()` - Verify S3 activation
- `testNewInstance_WithoutS3ArtifactDir()` - Verify fallback
- `testNewInstance_WithEmptyS3ArtifactDir()` - Verify empty property handling
- `testNewInstance_CreatesDirectories()` - Verify directory creation
- `testNewInstance_WithNonExistentParentDirectory()` - Verify nested path handling

## Deployment Components

### Helm Chart

**Location**: `s3-integration/helm-chart/`

**Components**:
- `Chart.yaml` - Chart metadata
- `values.yaml` - Configuration values
- `templates/pv-pvc.yaml` - S3 PV and PVC
- `templates/pod.yaml` - Maven build pod

### Docker Image

**Base Image**: `amazoncorretto:21-al2023`
**Additional**: Git installed
**Tag Format**: `3.9-amazoncorretto-21-al2023-s3`

### Build Script

**Location**: `build-image.sh`

**Steps**:
1. Parse region argument
2. Get AWS account ID
3. Build JAR with Maven
4. Authenticate to ECR (public and private)
5. Build Docker image
6. Push to ECR
