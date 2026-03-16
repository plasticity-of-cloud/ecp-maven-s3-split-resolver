# Storage Cost Comparison: Maven Artifacts (50GB)

## Executive Summary

Cost analysis for storing 50GB of Maven artifacts across AWS storage solutions for EKS workloads running 1000 jobs/day with bin packing optimization.

**Winner: S3 + Mountpoint with caching** - Most cost-effective at **$1.25/month** with excellent performance for read-heavy Maven workloads.

## Storage Options Compared

### 1. S3 Standard + Mountpoint for S3 (with caching)
- **Storage Cost**: $1.25/month (50GB × $0.025/GB-month)
- **Data Transfer**: ~$0 (within same AZ with caching)
- **Cache Storage**: 8GB EBS (emptyDir) - included in node costs
- **Total Monthly Cost**: **$1.25**

**Advantages:**
- Lowest cost for storage
- Excellent for read-heavy Maven workloads
- Automatic caching reduces repeated downloads
- Scales to zero when not in use
- No minimum provisioning requirements

### 2. Amazon EFS (General Purpose)
- **Storage Cost**: $16.50/month (50GB × $0.33/GB-month)
- **Data Transfer**: Included within VPC
- **Total Monthly Cost**: **$16.50**

**Advantages:**
- True POSIX filesystem
- Concurrent access from multiple pods
- No caching configuration needed
- Automatic scaling

### 3. Amazon FSx for Lustre (SSD, 50 MB/s per TB)
- **Storage Cost**: $7.00/month (50GB × $0.14/GB-month)
- **Minimum Size**: 1.2TB (~$168/month minimum)
- **Total Monthly Cost**: **$168** (due to minimum provisioning)

**Advantages:**
- High-performance parallel filesystem
- Excellent for HPC workloads
- Can integrate with S3 as data repository

## Workload Analysis: 1000 Jobs/Day with Bin Packing

### Access Patterns
- **Read-heavy**: Maven artifacts are downloaded, rarely modified
- **Bin packing**: Multiple jobs per node maximize cache efficiency
- **Temporal locality**: Same artifacts used across multiple jobs

### Performance Characteristics

| Storage Type | First Access | Cached Access | Concurrent Jobs |
|--------------|--------------|---------------|-----------------|
| S3 + Mountpoint | ~100-200ms | ~1-5ms | Excellent with cache |
| EFS | ~10-50ms | ~10-50ms | Good |
| FSx Lustre | ~1-10ms | ~1-10ms | Excellent |

### Cache Efficiency with Bin Packing
- **Cache hit ratio**: 80-90% for common Maven dependencies
- **8GB cache**: Sufficient for most frequently used artifacts
- **Node-local caching**: Reduces network traffic significantly

## Cost Breakdown (Monthly)

```
S3 + Mountpoint:     $1.25   (13x cheaper than EFS)
EFS:                $16.50   (baseline)
FSx Lustre:        $168.00   (134x more expensive)
```

## Recommendations

### Primary Choice: S3 + Mountpoint for S3
**Best for**: Maven artifact storage with 1000+ jobs/day

**Configuration:**
```yaml
mountOptions:
  - allow-delete
  - allow-overwrite
  - "metadata-ttl 300"
  - "read-part-size 1048576"
  - "max-threads 16"
  - uid=1000
  - gid=1000
cache: emptyDir
cacheEmptyDirSizeLimit: 8Gi
```

### Alternative: EFS for Complex Workflows
**Consider when**: Need true POSIX semantics or complex file operations

### Avoid: FSx Lustre for Maven Artifacts
**Reason**: Massive over-provisioning cost for this use case

## Implementation Notes

1. **S3 Lifecycle**: Configure lifecycle policies for old artifact versions
2. **Cache Tuning**: Monitor cache hit rates and adjust size if needed
3. **Regional Placement**: Ensure S3 bucket in same region as EKS cluster
4. **Monitoring**: Track storage costs and access patterns

## Cost Projections

| Scale | S3 + Mountpoint | EFS | FSx Lustre |
|-------|-----------------|-----|------------|
| 50GB | $1.25 | $16.50 | $168 |
| 100GB | $2.50 | $33.00 | $168 |
| 500GB | $12.50 | $165.00 | $168 |

*Note: FSx costs remain flat due to minimum 1.2TB provisioning*

---
*Analysis based on AWS pricing for Asia Pacific (Mumbai) region as of March 2026*
