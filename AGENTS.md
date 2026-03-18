# Maven S3 Split Resolver - AGENTS.md

## Overview

This document provides a starting point for AI assistants working with the Maven S3 Split Resolver codebase. It covers major subsystems, key entry points, and directory organization.

## Directory Overview

```
ecp-maven-s3-split-resolver/
├── .agents/summary/              # AI assistant documentation (generated)
│   ├── index.md                  # Start here for navigation
│   ├── codebase_info.md          # Project overview
│   ├── architecture.md           # System architecture
│   ├── components.md             # Component details
│   ├── interfaces.md             # API interfaces
│   ├── data_models.md            # Data structures
│   ├── workflows.md              # Execution workflows
│   ├── dependencies.md           # Dependencies
│   └── review_notes.md           # Documentation gaps
├── src/
│   ├── main/java/cloud/plasticity/maven/resolver/
│   │   ├── S3SplitLocalRepositoryManager.java    # Core resolver
│   │   └── S3SplitLocalRepositoryManagerFactory.java  # Factory
│   └── test/java/cloud/plasticity/maven/resolver/
│       ├── S3SplitLocalRepositoryManagerTest.java
│       └── S3SplitLocalRepositoryManagerFactoryTest.java
├── s3-integration/
│   └── helm-chart/               # Kubernetes deployment
│       ├── Chart.yaml
│       ├── values.yaml
│       └── templates/
├── build-image.sh                # Docker image builder
└── pom.xml                       # Maven configuration
```

## Key Entry Points

| File | Purpose |
|------|---------|
| `S3SplitLocalRepositoryManagerFactory.java` | Factory that activates the extension |
| `S3SplitLocalRepositoryManager.java` | Core resolver implementation |
| `build-image.sh` | Docker image builder script |
| `s3-integration/helm-chart/` | Kubernetes deployment |

## Repo-Specific Tools and Scripts

| Script | Purpose |
|--------|---------|
| `build-image.sh` | Builds and pushes Docker image to ECR |
| `s3-integration/clear_s3_bucket.py` | Clears S3 bucket contents |

## Patterns That Deviate from Defaults

### Dual Storage Architecture
- **Standard Maven**: Single local repository
- **This Extension**: Splits artifacts (S3) and metadata (EmptyDir)

### Zero-Copy File Transfer
- **Standard**: Standard file copy
- **This Extension**: Uses `FileChannel.transferTo()` for S3 uploads

### Symlink-Based Resolution
- **Standard**: Direct file access
- **This Extension**: Creates symlinks after S3 transfer

## Configuration Discovery

### System Properties
- `s3.resolver.artifactDir` - S3 mount path for artifacts
- `maven.repo.local` - EmptyDir path for metadata

### Maven Extension
- Configured in `.mvn/extensions.xml`
- Group: `cloud.plasticity`
- Artifact: `maven-s3-split-resolver`

## CI/CD Integration

### Build Process
1. `mvn clean package` - Build JAR
2. `./build-image.sh` - Build and push Docker image
3. `helm install` - Deploy to Kubernetes

### Test Process
- `mvn test` - Run JUnit 5 tests with Mockito

## Documentation Navigation

| Question | Read This File |
|----------|----------------|
| What does this project do? | `README.md` |
| How does it work internally? | `.agents/summary/architecture.md` |
| What are the key classes? | `.agents/summary/components.md` |
| What interfaces does it implement? | `.agents/summary/interfaces.md` |
| How does data flow? | `.agents/summary/data_models.md` |
| What's the execution flow? | `.agents/summary/workflows.md` |
| What dependencies are used? | `.agents/summary/dependencies.md` |

## Custom Instructions
<!-- This section is for human and agent-maintained operational knowledge.
     Add repo-specific conventions, gotchas, and workflow rules here.
     This section is preserved exactly as-is when re-running codebase-summary. -->
