# Storage Cost Comparison: Maven Artifacts (50GB)

## Executive Summary

Cost analysis for storing 50GB of Maven artifacts across AWS storage solutions for EKS workloads running 1000 jobs/day with bin packing optimization.

**Winner: S3 + Mountpoint with caching** - Most cost-effective at **$1.25-1.50/month** with excellent performance for read-heavy Maven workloads.

**Key Insight**: EFS per-operation costs ($0.03/GB read, $0.07/GB write) make it 21-31x more expensive than S3 for high-I/O Maven builds. FSx Lustre is single-AZ only with 1.2TB minimum provisioning.

## Storage Options Compared

### 1. S3 Standard + Mountpoint for S3 (with caching, Multi-AZ)
- **Storage Cost**: $1.25/month (50GB × $0.025/GB-month)
- **Data Transfer**: ~$0 (within same AZ with caching; cross-AZ: $0.01/GB)
- **Cache Storage**: 8GB EBS (emptyDir) - included in node costs
- **Cross-AZ Access**: Minimal cost due to high cache hit ratio (80-90%)
- **Total Monthly Cost**: **$1.25-1.50** (depending on cross-AZ cache misses)

**Advantages:**
- Lowest cost for storage
- Excellent for read-heavy Maven workloads
- Automatic caching reduces repeated downloads and cross-AZ traffic
- Scales to zero when not in use
- No minimum provisioning requirements
- S3 provides 11 9's durability across multiple AZs automatically

### 2. Amazon EFS (General Purpose, Multi-AZ with Elastic Throughput)
- **Storage Cost**: $16.50/month (50GB × $0.33/GB-month)
- **Read Operations**: $0.03 per GB transferred
- **Write Operations**: $0.07 per GB transferred  
- **Estimated throughput cost**: ~$15-30/month (500GB reads + 50GB writes for 1000 jobs/day)
- **Data Transfer**: Included within VPC (cross-AZ access included)
- **Total Monthly Cost**: **$31.50-46.50**

**Advantages:**
- True POSIX filesystem
- Concurrent access from multiple pods across AZs
- Automatic scaling
- Built-in multi-AZ replication and durability

**Disadvantages:**
- Per-operation costs add up quickly with high I/O workloads
- Maven builds generate many small read/write operations

### 3. Amazon FSx for Lustre (SSD, Single-AZ only)
- **Storage Cost**: $174/month (1.2TB minimum × $0.145/GB-month)
- **Throughput Cost**: Variable based on provisioned MBps ($0.52/MBps-month)
- **Metadata IOPS**: $0.055/IOPS-month (above included baseline)
- **Minimum Provisioning**: 1.2TB (cannot provision less)
- **Total Monthly Cost**: **$174+** (minimum, before throughput costs)

**Advantages:**
- High-performance parallel filesystem
- Excellent for HPC workloads
- Can integrate with S3 as data repository

**Disadvantages:**
- **Single-AZ only** - no multi-AZ option available
- Massive over-provisioning for 50GB use case (forced to pay for 1.2TB)
- Additional throughput costs scale quickly
- Not cost-effective for Maven artifact caching

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
S3 + Mountpoint:  $1.25-1.50   (21-31x cheaper than EFS, multi-AZ durable)
EFS:             $31.50-46.50  (baseline with operation costs, multi-AZ)
FSx Lustre:         $174.00+   (minimum provisioning, single-AZ only)
```

**Note:** EFS costs vary significantly based on I/O patterns. Maven builds with many small operations can push costs higher.

## Recommendations

### Primary Choice: S3 + Mountpoint for S3
**Best for**: Maven artifact storage with 1000+ jobs/day

**Configuration:**
```yaml
mountOptions:
  - allow-delete
  - allow-overwrite
  - "metadata-ttl 300"
  - "read-part-size 5242880"  # 5MB minimum required by Mountpoint
  - "max-threads 16"
  - uid=1000
  - gid=1000
cache: emptyDir
cacheEmptyDirSizeLimit: 8Gi
```

### Alternative: EFS for Complex Workflows
**Consider when**: Need true POSIX semantics or complex file operations

**Warning**: Per-operation costs can be significant for Maven builds. Monitor actual I/O patterns before committing.

### Avoid: FSx Lustre for Maven Artifacts
**Reasons**: 
- Single-AZ only (no multi-AZ option)
- Massive over-provisioning cost (1.2TB minimum for 50GB need)
- Throughput costs add up quickly

## Implementation Notes

1. **S3 Lifecycle**: Configure lifecycle policies for old artifact versions
2. **Cache Tuning**: Monitor cache hit rates and adjust size if needed
3. **Regional Placement**: Ensure S3 bucket in same region as EKS cluster
4. **Multi-AZ Considerations**: 
   - S3 is inherently multi-AZ durable (11 9's)
   - EFS provides multi-AZ access with no additional cost
   - Node-local caching minimizes cross-AZ data transfer costs
5. **Monitoring**: Track storage costs, access patterns, and cross-AZ traffic

## Cost Projections

| Scale | S3 + Mountpoint | EFS (Multi-AZ) | FSx Lustre (Single-AZ) |
|-------|-----------------|----------------|------------------------|
| 50GB | $1.25-1.50 | $31.50-46.50 | $174+ |
| 100GB | $2.50-3.00 | $63.00-93.00 | $174+ |
| 500GB | $12.50-15.00 | $315-465 | $174+ |

*Note: FSx costs remain flat due to minimum 1.2TB provisioning (single-AZ only)*
*S3 range accounts for cross-AZ data transfer on cache misses*
*EFS costs include storage + estimated operation costs (reads: $0.03/GB, writes: $0.07/GB)*

---
*Analysis based on AWS pricing for Asia Pacific (Mumbai) region as of March 2026*
