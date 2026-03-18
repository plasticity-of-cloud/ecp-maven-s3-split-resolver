# Maven S3 Split Resolver - Documentation Index

## Quick Start for AI Assistants

**Primary File to Read**: This file (`index.md`)

**How to Use This Documentation**:
1. Start here for overview and navigation
2. Use the table of contents below to find specific information
3. Each document contains metadata tags for targeted information retrieval
4. Cross-references link related documentation

## Documentation Overview

| File | Purpose | When to Read |
|------|---------|--------------|
| **index.md** | This file - navigation and overview | Start here for any question |
| **codebase_info.md** | High-level project information | Need project context |
| **architecture.md** | System architecture and design | Understanding system design |
| **components.md** | Component details and responsibilities | Working with specific components |
| **interfaces.md** | API interfaces and data models | Implementing or extending |
| **data_models.md** | Data structures and flows | Understanding data handling |
| **workflows.md** | Process workflows and sequences | Understanding execution flow |
| **dependencies.md** | Dependencies and compatibility | Version management |
| **review_notes.md** | Documentation gaps and issues | Identifying incomplete areas |

## Quick Reference

### Key Concepts

| Concept | Description |
|---------|-------------|
| **S3 Split Resolver** | Custom Maven extension separating artifacts (S3) from metadata (local) |
| **Mountpoint for S3** | AWS CSI driver for S3 file system access |
| **EmptyDir** | Kubernetes temporary storage for metadata |
| **Zero-Copy Transfer** | FileChannel.transferTo() for efficient S3 uploads |

### Configuration

| Property | Description |
|----------|-------------|
| `s3.resolver.artifactDir` | Path to S3 mount for artifacts |
| `maven.repo.local` | Path to EmptyDir for metadata |

### Core Classes

| Class | Purpose |
|-------|---------|
| `S3SplitLocalRepositoryManager` | Implements Maven's LocalRepositoryManager |
| `S3SplitLocalRepositoryManagerFactory` | Factory for creating the resolver |

## Documentation Details

### codebase_info.md

**Metadata Tags**: `#codebase #overview #technology-stack`

Contains:
- Project overview and description
- Technology stack (Java, Maven, Kubernetes, AWS)
- Project structure and file organization
- Key components and their purposes
- Architecture overview
- How it works (high-level)
- Configuration details
- Dependencies list
- Build and deployment commands
- Testing coverage
- Performance characteristics
- License and contact information

**Use for**: Getting started with the project, understanding the big picture

### architecture.md

**Metadata Tags**: `#architecture #design #components #data-flow`

Contains:
- System architecture diagrams
- Component architecture (class relationships)
- Data flow diagrams
- Storage separation
- Key design decisions
- Integration points
- Error handling

**Use for**: Understanding how the system works, design decisions, component relationships

### components.md

**Metadata Tags**: `#components #classes #interfaces #methods`

Contains:
- Core component details (S3SplitLocalRepositoryManager, Factory)
- Extension points (Maven SPI)
- Configuration components
- Storage components
- Test components
- Deployment components

**Use for**: Working with specific components, understanding component responsibilities

### interfaces.md

**Metadata Tags**: `#interfaces #api #maven-resolver #data-models`

Contains:
- Maven Resolver interfaces (LocalRepositoryManager, Factory)
- Maven data models (Artifact, Metadata, Request/Result types)
- SPI registration
- Configuration interfaces

**Use for**: Implementing or extending the extension, understanding API

### data_models.md

**Metadata Tags**: `#data-models #structures #flows #state`

Contains:
- Artifact path structures
- File type definitions
- Data flow models
- State models
- File transfer model
- Configuration models

**Use for**: Understanding data handling, file organization, state management

### workflows.md

**Metadata Tags**: `#workflows #sequences #processes #integration`

Contains:
- Build workflow
- Artifact transfer workflow
- Artifact resolution workflow
- Factory activation workflow
- Directory setup workflow
- Test workflows
- Deployment workflows
- Error recovery workflow

**Use for**: Understanding execution flow, debugging, integration

### dependencies.md

**Metadata Tags**: `#dependencies #versions #compatibility #security`

Contains:
- Maven dependencies (provided and test)
- External services (AWS S3, Kubernetes)
- Build dependencies
- Runtime dependencies
- Dependency graph
- Version compatibility
- Dependency management
- Security dependencies

**Use for**: Version management, compatibility checks, security review

## Cross-References

### From Codebase Info
- See **architecture.md** for detailed architecture
- See **components.md** for component details
- See **dependencies.md** for dependency information

### From Architecture
- See **components.md** for component details
- See **interfaces.md** for interface definitions
- See **data_models.md** for data structures
- See **workflows.md** for execution flow

### From Components
- See **architecture.md** for component relationships
- See **interfaces.md** for interface definitions
- See **data_models.md** for data structures
- See **workflows.md** for component workflows

### From Interfaces
- See **components.md** for implementation details
- See **data_models.md** for data model usage
- See **workflows.md** for interface usage in workflows

### From Data Models
- See **architecture.md** for data flow context
- See **components.md** for data model usage
- See **workflows.md** for data flow in execution

### From Workflows
- See **components.md** for component roles in workflows
- See **data_models.md** for data structures used
- See **interfaces.md** for interface calls in workflows

### From Dependencies
- See **codebase_info.md** for version overview
- See **architecture.md** for integration points
- See **components.md** for component dependencies

## Example Queries

### "How does the artifact transfer work?"
1. Read **workflows.md** → Artifact Transfer Workflow
2. See **data_models.md** → File Transfer Model
3. See **components.md** → S3SplitLocalRepositoryManager → zeroCopyFile()

### "What interfaces does this implement?"
1. Read **interfaces.md** → Maven Resolver Interfaces
2. See **components.md** → Core Components → S3SplitLocalRepositoryManager
3. See **architecture.md** → Component Architecture

### "How do I configure this extension?"
1. Read **codebase_info.md** → Configuration
2. See **components.md** → Configuration Components
3. See **workflows.md** → Build Workflow

### "What are the key classes?"
1. Read **codebase_info.md** → Key Components
2. See **components.md** → Core Components
3. See **architecture.md** → Component Architecture

### "How does it integrate with Maven?"
1. Read **interfaces.md** → SPI Registration
2. See **workflows.md** → Build Workflow
3. See **architecture.md** → Integration Points

## Maintenance Notes

### Documentation Generation
- Generated by: codebase-summary agent
- Last updated: 2026-03-17
- Source: `/home/ubuntu/projects/ecp/ecp-maven-s3-split-resolver`

### Adding Documentation
- Add new files to `.agents/summary/`
- Update this index with new files
- Add cross-references as needed

### Updating Documentation
- Re-run codebase-summary to regenerate
- Review changes before committing
- Update manual sections (Custom Instructions)

## Custom Instructions
<!-- This section is for human and agent-maintained operational knowledge.
     Add repo-specific conventions, gotchas, and workflow rules here.
     This section is preserved exactly as-is when re-running codebase-summary. -->
