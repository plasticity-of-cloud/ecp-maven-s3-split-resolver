# Maven S3 Split Resolver - Data Models

## Core Data Models

### Artifact Path Structure

**S3 Storage** (`artifactDir`):
```
{groupId}/{artifactId}/{version}/{artifactId}-{version}.{extension}
```

**Example**:
```
org/apache/commons/commons-lang3/3.12.0/commons-lang3-3.12.0.jar
org/apache/commons/commons-lang3/3.12.0/commons-lang3-3.12.0.pom
org/apache/commons/commons-lang3/3.12.0/commons-lang3-3.12.0.jar.sha1
```

**EmptyDir Storage** (`metadataDir`):
```
{groupId}/{artifactId}/{version}/_remote.repositories
{groupId}/{artifactId}/{version}/.lastUpdated
{groupId}/{artifactId}/{version}/resolver-status.properties
```

### Artifact File Types

| Type | Extension | Description |
|------|-----------|-------------|
| Main artifact | `.jar` | The primary artifact (JAR, WAR, etc.) |
| POM file | `.pom` | Maven POM file |
| SHA1 checksum | `.sha1` | SHA-1 checksum |
| MD5 checksum | `.md5` | MD5 checksum |

### Metadata File Types

| File | Purpose |
|------|---------|
| `_remote.repositories` | Tracks which remote repository each artifact came from |
| `.lastUpdated` | Timestamp of last update for snapshot versions |
| `resolver-status.properties` | Resolver state and status information |

## Data Flow Models

### Artifact Write Flow

```mermaid
flowchart TD
    A[Artifact Created in EmptyDir] --> B{File Exists?}
    B -->|Yes| C{Is Symlink?}
    C -->|No| D[Create Parent Directories]
    D --> E[Zero-Copy Transfer to S3]
    E --> F[Delete Source File]
    F --> G[Create Symlink to S3]
    C -->|Yes| H[Skip - Already on S3]
    B -->|No| I[Skip - No File]
```

### Artifact Read Flow

```mermaid
flowchart TD
    A[Artifact Request] --> B[Check Delegate First]
    B --> C{Found?}
    C -->|Yes| D[Return Result]
    C -->|No| E[Check S3 Directory]
    E --> F{File Exists?}
    F -->|Yes| G[Set Available = true]
    G --> H[Return S3 File Path]
    F -->|No| I[Return Not Found]
```

### Factory Activation Flow

```mermaid
flowchart TD
    A[Maven Calls Factory] --> B{Property Set?}
    B -->|Yes & Non-Empty| C[Create S3SplitManager]
    B -->|No or Empty| D[Fallback to Delegate]
    C --> E[Store artifactDir]
    E --> F[Store metadataDir]
    F --> G[Store Delegate]
    G --> H[Return Manager]
```

## State Models

### S3SplitLocalRepositoryManager State

```mermaid
classDiagram
    class S3SplitLocalRepositoryManager {
        -LocalRepositoryManager delegate
        -Path artifactDir
        -Path metadataDir
        -LocalRepository localRepository
    }
    
    class LocalRepository {
        -File basedir
    }
    
    S3SplitLocalRepositoryManager --> LocalRepository : localRepository
```

### Repository Paths

| Path Type | Variable | Example |
|-----------|----------|---------|
| S3 Artifact Directory | `artifactDir` | `/home/maven/.m2/repository` |
| EmptyDir Metadata Directory | `metadataDir` | `/home/maven/.m2-metadata/repository-metadata` |
| Local Repository Base | `localRepository.basedir` | Same as `metadataDir` |

## File Transfer Model

### Zero-Copy Transfer

**Implementation**:
```java
try (FileChannel srcChannel = FileChannel.open(source, StandardOpenOption.READ);
     FileChannel dstChannel = FileChannel.open(dest, StandardOpenOption.WRITE, 
                                               StandardOpenOption.CREATE, 
                                               StandardOpenOption.TRUNCATE_EXISTING)) {
    long size = srcChannel.size();
    long transferred = 0;
    while (transferred < size) {
        transferred += srcChannel.transferTo(transferred, size - transferred, dstChannel);
    }
}
```

**Characteristics**:
- Uses `FileChannel.transferTo()` for OS-level optimization
- Leverages `sendfile()` system call when available
- No user-space buffering required
- Efficient for large files

### Symlink Creation

**Pattern**:
```
Source: /home/maven/.m2-metadata/repository/org/example/artifact/1.0/artifact-1.0.jar
Target: /home/maven/.m2/repository/org/example/artifact/1.0/artifact-1.0.jar
```

**Purpose**:
- Fast artifact resolution from S3
- No duplicate storage
- Transparent access

## Error State Models

### Transfer Failure Handling

```mermaid
flowchart TD
    A[Transfer Attempt] --> B{Success?}
    B -->|Yes| C[Delete Source]
    C --> D[Create Symlink]
    B -->|No| E[IOException]
    E --> F[Log Warning]
    F --> G[Continue Build]
```

### Duplicate File Handling

```mermaid
flowchart TD
    A[File Exists] --> B{Already Symlink?}
    B -->|Yes| C[Skip Transfer]
    B -->|No| D{Target Exists?}
    D -->|Yes| E[Delete Source]
    E --> F[Create Symlink]
    D -->|No| G[Transfer File]
```

## Configuration Models

### System Property Configuration

| Property | Type | Required | Default | Description |
|----------|------|----------|---------|-------------|
| `s3.resolver.artifactDir` | String | No | None | S3 mount path for artifacts |
| `maven.repo.local` | String | No | ~/.m2/repository | EmptyDir path for metadata |

### Priority Model

| Component | Priority | Description |
|-----------|----------|-------------|
| Default Manager | 0.0 | Maven's default local repository manager |
| S3 Split Manager | 100.0 | Custom S3-aware manager (higher priority) |
