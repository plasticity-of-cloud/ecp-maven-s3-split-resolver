# Maven S3 Integration Helm Chart

Deploy Maven build pods with S3-backed artifact caching using Mountpoint for S3.

## Installation

```bash
helm install maven-s3 ./helm-chart -n elastic-cicd --create-namespace
```

## Configuration

Key values to customize in `values.yaml`:

### Image Configuration
```yaml
image:
  repository: your-registry/maven
  tag: 3.9-amazoncorretto-21-al2023-s3
```

### S3 Configuration
```yaml
s3:
  bucketName: your-bucket-name
  region: your-region
```

### Resources
```yaml
resources:
  requests:
    cpu: "2"
    memory: "2Gi"
  limits:
    cpu: "3"
    memory: "4Gi"
```

## Uninstall

```bash
helm uninstall maven-s3 -n elastic-cicd
```
