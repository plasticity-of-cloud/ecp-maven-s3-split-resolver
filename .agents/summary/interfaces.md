# Maven S3 Split Resolver - Interfaces

## Maven Resolver Interfaces

### LocalRepositoryManager

**Package**: `org.eclipse.aether.repository`

**Purpose**: Interface for managing local repository operations in Maven.

**Methods**:

| Method | Description |
|--------|-------------|
| `getPathForLocalArtifact(Artifact)` | Get relative path for artifact in local repo |
| `getPathForRemoteArtifact(Artifact, RemoteRepository, String)` | Get path for artifact from remote repo |
| `getPathForLocalMetadata(Metadata)` | Get relative path for metadata in local repo |
| `getPathForRemoteMetadata(Metadata, RemoteRepository, String)` | Get path for metadata from remote repo |
| `find(RepositorySystemSession, LocalArtifactRequest)` | Check if artifact exists locally |
| `find(RepositorySystemSession, LocalMetadataRequest)` | Check if metadata exists locally |
| `add(RepositorySystemSession, LocalArtifactRegistration)` | Register artifact in local repo |
| `add(RepositorySystemSession, LocalMetadataRegistration)` | Register metadata in local repo |
| `getRepository()` | Get the local repository configuration |

**Usage in Extension**:
- `S3SplitLocalRepositoryManager` implements this interface
- Delegates most methods to the original manager
- Overrides `find()` and `add()` for artifact routing

### LocalRepositoryManagerFactory

**Package**: `org.eclipse.aether.spi.localrepo`

**Purpose**: Factory interface for creating LocalRepositoryManager instances.

**Methods**:

| Method | Description |
|--------|-------------|
| `newInstance(RepositorySystemSession, LocalRepository)` | Create a new LocalRepositoryManager |
| `getPriority()` | Get priority for SPI selection |

**Usage in Extension**:
- `S3SplitLocalRepositoryManagerFactory` implements this interface
- Priority: 100.0 (higher than default)
- Only creates S3SplitLocalRepositoryManager when configured

## Maven Data Models

### Artifact

**Package**: `org.eclipse.aether.artifact`

**Key Properties**:
- `groupId`: Group ID (e.g., "org.apache.commons")
- `artifactId`: Artifact ID (e.g., "commons-lang3")
- `version`: Version (e.g., "3.12.0")
- `classifier`: Classifier (optional)
- `extension`: File extension (default: "jar")

**Usage**: Used to locate and manage artifacts in the repository.

### Metadata

**Package**: `org.eclipse.aether.metadata`

**Key Properties**:
- `groupId`: Group ID
- `artifactId`: Artifact ID
- `version`: Version
- `type`: Metadata type (e.g., "maven-metadata.xml")

**Usage**: Used for tracking metadata files like `_remote.repositories`.

### LocalArtifactRequest

**Package**: `org.eclipse.aether.repository`

**Properties**:
- `artifact`: The artifact to find
- `repositories`: List of remote repositories
- `context`: Request context

**Usage**: Passed to `find()` method to request artifact lookup.

### LocalMetadataRequest

**Package**: `org.eclipse.aether.repository`

**Properties**:
- `metadata`: The metadata to find
- `repositories`: List of remote repositories
- `context`: Request context

**Usage**: Passed to `find()` method to request metadata lookup.

### LocalArtifactRegistration

**Package**: `org.eclipse.aether.repository`

**Properties**:
- `artifact`: The artifact being registered
- `file`: The artifact file
- `pomFile`: The POM file (optional)

**Usage**: Passed to `add()` method to register a new artifact.

### LocalMetadataRegistration

**Package**: `org.eclipse.aether.repository`

**Properties**:
- `metadata`: The metadata being registered
- `file`: The metadata file

**Usage**: Passed to `add()` method to register new metadata.

## Result Types

### LocalArtifactResult

**Package**: `org.eclipse.aether.repository`

**Properties**:
- `artifact`: The artifact
- `file`: The artifact file (if found)
- `available`: Whether artifact is available
- `repository`: The repository containing the artifact

**Usage**: Returned by `find()` method for artifact requests.

### LocalMetadataResult

**Package**: `org.eclipse.aether.repository`

**Properties**:
- `metadata`: The metadata
- `file`: The metadata file (if found)
- `available`: Whether metadata is available
- `repository`: The repository containing the metadata

**Usage**: Returned by `find()` method for metadata requests.

## SPI Registration

### Service Provider Configuration

**File**: `META-INF/services/org.eclipse.aether.spi.localrepo.LocalRepositoryManagerFactory`

**Content**:
```
cloud.plasticity.maven.resolver.S3SplitLocalRepositoryManagerFactory
```

### Maven Extension Configuration

**File**: `.mvn/extensions.xml`

**Content**:
```xml
<extensions>
  <extension>
    <groupId>cloud.plasticity</groupId>
    <artifactId>maven-s3-split-resolver</artifactId>
    <version>1.0.0</version>
  </extension>
</extensions>
```

## Configuration Interfaces

### System Property Configuration

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `s3.resolver.artifactDir` | String | No | Path to S3 mount for artifacts |
| `maven.repo.local` | String | No | Path to metadata storage |

### Environment Variables

| Variable | Description |
|----------|-------------|
| `MAVEN_OPTS` | JVM options including system properties |
| `HOME` | Home directory for Maven user |
| `USER` | Username for file permissions |
