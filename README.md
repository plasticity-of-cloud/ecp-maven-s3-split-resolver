# Maven S3 Split Resolver

Custom Maven Resolver extension that separates artifact storage from metadata storage.

## Problem

Mountpoint for S3 only supports sequential writes (no random I/O, no in-place updates). Maven's metadata files (`_remote.repositories`, `.lastUpdated`, `resolver-status.properties`) use random I/O patterns that fail on S3.

## Solution

This extension splits the local repository into two locations:
- **Artifacts** (JARs, POMs, checksums) → S3 mount (immutable, sequential writes)
- **Metadata** (tracking files) → EmptyDir (mutable, random I/O)

## Usage

### 1. Build and install

```bash
mvn clean install
```

### 2. Install the extension

Copy the built JAR into Maven's extension directory:

```bash
cp target/maven-s3-split-resolver-1.0.0-SNAPSHOT.jar $MAVEN_HOME/lib/ext/
```

Alternatively, reference it via `.mvn/extensions.xml` in your project:

```xml
<extensions>
  <extension>
    <groupId>cloud.plasticity</groupId>
    <artifactId>maven-s3-split-resolver</artifactId>
    <version>1.0.0-SNAPSHOT</version>
  </extension>
</extensions>
```

### 3. Configure paths

Set system properties in `MAVEN_OPTS`:

```bash
export MAVEN_OPTS="-Dmaven.repo.local=/home/maven/.m2/repository-metadata \
                   -Ds3.resolver.artifactDir=/home/maven/.m2/repository"
```

Or in pod YAML:

```yaml
env:
- name: MAVEN_OPTS
  value: "-Dmaven.repo.local=/home/maven/.m2/repository-metadata -Ds3.resolver.artifactDir=/home/maven/.m2/repository"
```

## How it works

- `maven.repo.local` → EmptyDir mount (metadata base directory)
- `s3.resolver.artifactDir` → S3 mount (artifact storage)
- The custom `LocalRepositoryManager` routes artifact writes to S3, metadata writes to EmptyDir
- All tracking files naturally stay in EmptyDir with full POSIX I/O support

## Architecture

```
/home/maven/.m2/repository          (S3 mount)
├── org/apache/commons/.../*.jar    ← Artifacts here
└── org/apache/commons/.../*.pom

/home/maven/.m2/repository-metadata (EmptyDir)
├── org/apache/commons/.../_remote.repositories  ← Metadata here
└── org/apache/commons/.../.lastUpdated
```

## License

This project is dual-licensed:

- **AGPL-3.0** — Free for open-source use under the terms of the [GNU Affero General Public License v3.0](https://www.gnu.org/licenses/agpl-3.0.html).
- **Commercial License** — For proprietary/internal use without AGPL obligations. See [COMMERCIAL-LICENSE.md](COMMERCIAL-LICENSE.md) for terms and pricing.

For SaaS usage or custom commercial agreements, contact **ecosystem@plasticity.cloud**.
