# CodeBuild Alternative: S3 Caching

## Why Not Mountpoint for S3?

CodeBuild containers don't support FUSE, which Mountpoint requires. Instead, use CodeBuild's built-in S3 caching.

## S3 Caching Configuration

```yaml
# buildspec.yml
version: 0.2

cache:
  type: s3
  paths:
    - '/root/.m2/**/*'
  location: 'your-bucket-name/codebuild-cache'

phases:
  build:
    commands:
      - mvn clean package
```

## Benefits

- No FUSE required
- Works natively in CodeBuild
- Cross-host caching
- Automatic cache invalidation

## Limitations

- 10GB cache limit
- Not a full S3 mount (only specific paths)
