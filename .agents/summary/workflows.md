# Maven S3 Split Resolver - Workflows

## Build Workflow

```mermaid
sequenceDiagram
    participant Maven as Maven Build
    participant Factory as S3SplitLocalRepositoryManagerFactory
    participant Manager as S3SplitLocalRepositoryManager
    participant S3 as S3 Mount
    participant EmptyDir as EmptyDir
    
    Maven->>Factory: Initialize Repository System
    Factory->>Factory: Check s3.resolver.artifactDir property
    alt Property Set
        Factory->>Manager: Create S3SplitLocalRepositoryManager
        Manager->>Manager: Store artifactDir (S3)
        Manager->>Manager: Store metadataDir (EmptyDir)
        Manager->>Manager: Create delegate manager
        Manager-->>Factory: Return S3SplitLocalRepositoryManager
    else Property Not Set
        Factory-->>Factory: Use default manager
    end
    
    Maven->>Manager: Process Dependencies
    loop For each artifact
        Manager->>Manager: Download to EmptyDir
        Manager->>Manager: Call add(artifact)
        Manager->>S3: Copy artifact
        Manager->>EmptyDir: Delete source
        Manager->>EmptyDir: Create symlink
    end
    
    Maven->>Manager: Build Project
    Manager->>S3: Resolve artifacts via symlink
```

## Artifact Transfer Workflow

```mermaid
sequenceDiagram
    participant Maven as Maven Build
    participant Manager as S3SplitLocalRepositoryManager
    participant S3 as S3 Mount
    participant EmptyDir as EmptyDir
    
    Maven->>Manager: add(LocalArtifactRegistration)
    Manager->>Manager: Get artifact path
    Manager->>EmptyDir: Check if file exists
    alt File Exists
        Manager->>EmptyDir: Check if symlink
        alt Not Symlink
            Manager->>Manager: Create parent directories in S3
            Manager->>S3: Zero-copy transfer
            Manager->>EmptyDir: Delete source file
            Manager->>EmptyDir: Create symlink to S3
        else Already Symlink
            Manager->>Manager: Skip - already on S3
        end
    else No File
        Manager->>Manager: Skip - nothing to transfer
    end
```

## Artifact Resolution Workflow

```mermaid
sequenceDiagram
    participant Maven as Maven Build
    participant Manager as S3SplitLocalRepositoryManager
    participant S3 as S3 Mount
    participant EmptyDir as EmptyDir
    
    Maven->>Manager: find(LocalArtifactRequest)
    Manager->>Manager: Check delegate first
    alt Found in Delegate
        Manager-->>Maven: Return LocalArtifactResult
    else Not Found
        Manager->>S3: Check if artifact exists
        alt Found in S3
            Manager->>S3: Get file path
            Manager-->>Maven: Return available result with S3 path
        else Not Found
            Manager-->>Maven: Return not found result
        end
    end
```

## Factory Activation Workflow

```mermaid
sequenceDiagram
    participant Maven as Maven Build
    participant Factory as S3SplitLocalRepositoryManagerFactory
    participant Manager as S3SplitLocalRepositoryManager
    
    Maven->>Factory: newInstance(session, repository)
    Factory->>Factory: Get s3.resolver.artifactDir property
    alt Property Set & Non-Empty
        Factory->>Factory: Create S3SplitLocalRepositoryManager
        Factory->>Manager: Store artifactDir
        Factory->>Manager: Store metadataDir
        Factory->>Manager: Store delegate
        Manager-->>Factory: Return new instance
    else Property Not Set or Empty
        Factory->>Factory: Fall back to delegate
        Factory-->>Factory: Return delegate manager
    end
```

## Directory Setup Workflow

```mermaid
flowchart TD
    A[Build Starts] --> B[Create EmptyDir Volume]
    B --> C[Create S3 Mount Volume]
    C --> D[Set System Properties]
    D --> E[Initialize Factory]
    E --> F[Create Manager with Paths]
    F --> G[Build Proceeds]
```

## Test Workflow

### Unit Test: Artifact Transfer

```mermaid
sequenceDiagram
    participant Test as Test
    participant Manager as S3SplitLocalRepositoryManager
    participant S3 as S3 Mount
    participant EmptyDir as EmptyDir
    
    Test->>EmptyDir: Create test artifact
    Test->>Manager: add(artifactRegistration)
    Manager->>S3: Transfer artifact
    Manager->>EmptyDir: Delete source
    Manager->>EmptyDir: Create symlink
    Test->>S3: Verify artifact exists
    Test->>EmptyDir: Verify symlink created
```

### Integration Test: End-to-End

```mermaid
sequenceDiagram
    participant Test as Test
    participant Maven as Maven Build
    participant Manager as S3SplitLocalRepositoryManager
    participant S3 as S3 Mount
    participant EmptyDir as EmptyDir
    
    Test->>Maven: Start build with extension
    Maven->>Manager: Initialize S3SplitLocalRepositoryManager
    Manager->>Manager: Configure paths
    Maven->>EmptyDir: Download dependencies
    Maven->>Manager: Add artifacts
    Manager->>S3: Transfer artifacts
    Manager->>EmptyDir: Create symlinks
    Maven->>S3: Resolve artifacts
    S3-->>Maven: Return artifacts
    Maven-->>Test: Build complete
```

## Deployment Workflow

### Helm Chart Deployment

```mermaid
sequenceDiagram
    participant User as User
    participant Helm as Helm
    participant K8s as Kubernetes
    
    User->>Helm: helm install maven-s3
    Helm->>K8s: Create namespace
    K8s-->>Helm: Namespace created
    Helm->>K8s: Create PV (S3)
    K8s-->>Helm: PV created
    Helm->>K8s: Create PVC
    K8s-->>Helm: PVC bound
    Helm->>K8s: Create Pod
    K8s-->>Helm: Pod running
```

### Docker Build Workflow

```mermaid
sequenceDiagram
    participant User as User
    participant Script as build-image.sh
    participant Maven as Maven
    participant ECR as ECR
    
    User->>Script: ./build-image.sh
    Script->>Script: Parse arguments
    Script->>Maven: mvn clean package
    Maven-->>Script: JAR built
    Script->>ECR: Authenticate (public)
    Script->>ECR: Authenticate (private)
    Script->>Script: docker build
    Script->>ECR: docker push
    ECR-->>Script: Image pushed
    Script-->>User: Build complete
```

## Error Recovery Workflow

```mermaid
flowchart TD
    A[Transfer Attempt] --> B{File Exists?}
    B -->|No| C[Skip - No File]
    B -->|Yes| D{Already Symlink?}
    D -->|Yes| E[Skip - Already on S3]
    D -->|No| F[Create Directories]
    F --> G{Transfer Success?}
    G -->|No| H[Log Warning]
    H --> I[Continue Build]
    G -->|Yes| J[Delete Source]
    J --> K{Symlink Success?}
    K -->|No| L[Log Warning]
    L --> I
    K -->|Yes| M[Artifact Available on S3]
```
