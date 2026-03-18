# Maven S3 Split Resolver - Architecture

## System Architecture

```mermaid
graph TB
    subgraph "Maven Build Process"
        A1[Maven Build] --> A2[Repository System]
        A2 --> A3[LocalRepositoryManager]
    end
    
    subgraph "S3 Split Resolver Extension"
        A3 --> B1[S3SplitLocalRepositoryManagerFactory]
        B1 --> B2[S3SplitLocalRepositoryManager]
    end
    
    subgraph "Storage Layer"
        B2 --> C1[S3 Mount<br/>Artifacts: JARs, POMs, Checksums]
        B2 --> C2[EmptyDir<br/>Metadata: _remote.repositories, .lastUpdated]
    end
    
    A1 -->|Uses| B2
```

## Component Architecture

```mermaid
classDiagram
    class LocalRepositoryManager {
        +getPathForLocalArtifact(Artifact)
        +getPathForRemoteArtifact(Artifact, RemoteRepository, String)
        +getPathForLocalMetadata(Metadata)
        +getPathForRemoteMetadata(Metadata, RemoteRepository, String)
        +find(RepositorySystemSession, LocalArtifactRequest)
        +find(RepositorySystemSession, LocalMetadataRequest)
        +add(RepositorySystemSession, LocalArtifactRegistration)
        +add(RepositorySystemSession, LocalMetadataRegistration)
        +getRepository()
    }
    
    class S3SplitLocalRepositoryManager {
        -LocalRepositoryManager delegate
        -Path artifactDir
        -Path metadataDir
        -LocalRepository localRepository
        +find(session, request)
        +add(session, request)
        +zeroCopyFile(source, dest)
    }
    
    class S3SplitLocalRepositoryManagerFactory {
        -LocalRepositoryManagerFactory delegate
        +newInstance(session, repository)
        +getPriority()
    }
    
    class LocalRepositoryManagerFactory {
        <<Interface>>
        +newInstance(session, repository)
        +getPriority()
    }
    
    S3SplitLocalRepositoryManager ..|> LocalRepositoryManager
    S3SplitLocalRepositoryManagerFactory ..|> LocalRepositoryManagerFactory
    S3SplitLocalRepositoryManagerFactory --> S3SplitLocalRepositoryManager : creates
    S3SplitLocalRepositoryManager --> LocalRepositoryManager : delegates
```

## Data Flow

```mermaid
sequenceDiagram
    participant Maven as Maven Build
    participant Factory as S3SplitLocalRepositoryManagerFactory
    participant Manager as S3SplitLocalRepositoryManager
    participant S3 as S3 Mount
    participant EmptyDir as EmptyDir
    
    Maven->>Factory: newInstance(session, repository)
    Factory->>Factory: Check s3.resolver.artifactDir property
    alt Property Set
        Factory->>Manager: Create new instance
        Manager->>Manager: Store artifactDir (S3)
        Manager->>Manager: Store metadataDir (EmptyDir)
        Manager->>Manager: Create delegate manager
        Manager-->>Factory: Return S3SplitLocalRepositoryManager
    else Property Not Set
        Factory-->>Factory: Fall back to delegate
    end
    
    Maven->>Manager: find(session, artifactRequest)
    Manager->>Manager: Check delegate first
    Manager->>S3: Check if artifact exists
    S3-->>Manager: Artifact found/not found
    Manager-->>Maven: LocalArtifactResult
    
    Maven->>Manager: add(session, artifactRegistration)
    Manager->>Manager: Delegate to local manager
    Manager->>Manager: Copy artifact to S3
    Manager->>S3: Write artifact
    Manager->>Manager: Delete local file
    Manager->>Manager: Create symlink to S3
    Manager-->>Maven: Complete
```

## Storage Separation

```mermaid
graph LR
    subgraph "Metadata Storage (EmptyDir)"
        M1["_remote.repositories"]
        M2[".lastUpdated"]
        M3["resolver-status.properties"]
    end
    
    subgraph "Artifact Storage (S3)"
        A1["*.jar"]
        A2["*.pom"]
        A3["*.sha1"]
        A4["*.md5"]
    end
    
    M1 -->|Stored in| EmptyDir
    M2 -->|Stored in| EmptyDir
    M3 -->|Stored in| EmptyDir
    A1 -->|Stored in| S3
    A2 -->|Stored in| S3
    A3 -->|Stored in| S3
    A4 -->|Stored in| S3
```

## Key Design Decisions

### 1. Why Split Storage?

Mountpoint for S3 only supports sequential writes. Maven's metadata files use random I/O patterns that fail on S3.

### 2. Why EmptyDir for Metadata?

EmptyDir provides:
- Full POSIX I/O support
- Fast random read/write operations
- Temporary storage suitable for build artifacts

### 3. Why Zero-Copy Transfer?

`FileChannel.transferTo()` leverages OS-level optimizations (sendfile) for efficient file transfers without copying to user space.

### 4. Why Symlinks?

After transfer, symlinks allow:
- Fast artifact resolution from S3
- No duplicate storage
- Transparent access to artifacts

## Integration Points

```mermaid
graph TB
    subgraph "Maven Ecosystem"
        A[Maven Build] --> B[Repository System]
        B --> C[LocalRepositoryManager SPI]
    end
    
    subgraph "AWS Ecosystem"
        D[Mountpoint for S3] --> E[S3 Bucket]
    end
    
    subgraph "Kubernetes"
        F[Pod] --> G[EmptyDir Volume]
        F --> H[PVC - S3 CSI Driver]
    end
    
    C -->|Uses| D
    G -->|Stores| M1[Metadata Files]
    H -->|Stores| A1[Artifact Files]
```

## Error Handling

```mermaid
flowchart TD
    A[Artifact Write] --> B{File Exists?}
    B -->|Yes| C{Is Symlink?}
    C -->|No| D[Create Directories]
    D --> E[Zero-Copy Transfer]
    E --> F[Delete Source]
    F --> G[Create Symlink]
    C -->|Yes| H[Skip - Already on S3]
    B -->|No| I[Skip - No file to copy]
    E -->|IOException| J[Log Warning]
    J --> K[Continue Build]
```
