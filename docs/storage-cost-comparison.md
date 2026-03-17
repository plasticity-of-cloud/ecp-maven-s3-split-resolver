# Storage Cost Comparison: Maven Artifacts (50GB)

## Executive Summary

Cost analysis for storing 50GB of Maven artifacts across AWS storage solutions for EKS workloads running 1000 jobs/day with bin packing optimization.

**Winner: S3 + Mountpoint with caching** - Most cost-effective at **$1.25-1.50/month** with excellent performance for read-heavy Maven workloads.

**Key Insights**: 
- EFS per-operation costs ($0.03/GB read, $0.07/GB write) make it 21-31x more expensive than S3 for high-I/O Maven builds
- FSx OpenZFS minimum throughput provisioning (80 MBps) drives costs to $78.60+/month, making it 52-63x more expensive than S3

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

### 3. Amazon FSx for OpenZFS (Multi-AZ, SSD)
- **Storage Cost**: $9.00/month (50GB × $0.18/GB-month for Multi-AZ SSD)
- **Throughput Cost**: $69.60/month (80 MBps × $0.87/MBps-month minimum)
- **Minimum Throughput**: 80 MBps (cannot provision less for Multi-AZ)
- **Data Transfer**: Included in throughput price (cross-AZ replication included)
- **Total Monthly Cost**: **$78.60+**

**Advantages:**
- True POSIX filesystem with ZFS features
- Multi-AZ high availability with automatic failover
- High performance (sub-millisecond latency)
- Snapshots and cloning capabilities

**Disadvantages:**
- Minimum throughput provisioning drives cost
- Significantly more expensive than S3 for artifact caching
- Over-provisioned for Maven workload needs

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

### Low Throughput (Baseline)
```
S3 + Mountpoint:  $1.25-1.50   (52-63x cheaper than FSx OpenZFS, multi-AZ durable)
EFS:             $31.50-46.50  (baseline with operation costs, multi-AZ)
FSx OpenZFS:        $78.60+    (minimum with 80 MBps throughput, multi-AZ HA)
```

### High Throughput (500 MB/s sustained, 8 hours/day)
```
S3 + Mountpoint:     $1.25-1.50   (scales automatically, no throughput charges)
FSx OpenZFS:           $444.00    (500 MBps × $0.87/MBps + storage, multi-AZ)
EFS:                $16,384.50    (432,000 GB transferred × $0.03-0.07/GB, multi-AZ)
```

**Key Insight:** At high throughput, S3 is **296x cheaper than FSx OpenZFS** and **10,923x cheaper than EFS**. EFS per-GB transfer pricing becomes prohibitively expensive for high-throughput workloads.

**Note:** EFS costs vary significantly based on I/O patterns. Maven builds with many small operations can push costs higher.

## Recommendations

### Primary Choice: S3 + Mountpoint for S3
**Best for**: Maven artifact storage with 1000+ jobs/day

**Why it wins:**
- Lowest cost at any scale ($1.25-1.50/month for 50GB)
- Throughput scales automatically without additional charges
- At high throughput (500 MB/s), 296x cheaper than FSx OpenZFS, 10,923x cheaper than EFS
- Multi-AZ durable by default (11 9's)
- Node-local caching provides excellent performance

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

### Avoid: FSx OpenZFS for Maven Artifacts
**Reasons**: 
- Minimum throughput provisioning (80 MBps) drives cost to $78.60+/month
- Over-provisioned for Maven artifact caching workload
- 52-63x more expensive than S3 solution

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

| Scale | S3 + Mountpoint | EFS (Multi-AZ) | FSx OpenZFS (Multi-AZ) |
|-------|-----------------|----------------|------------------------|
| 50GB | $1.25-1.50 | $31.50-46.50 | $78.60+ |
| 100GB | $2.50-3.00 | $63.00-93.00 | $87.60+ |
| 500GB | $12.50-15.00 | $315-465 | $159.60+ |

*S3 range accounts for cross-AZ data transfer on cache misses*
*EFS costs include storage + estimated operation costs (reads: $0.03/GB, writes: $0.07/GB)*
*FSx OpenZFS includes minimum 80 MBps throughput ($69.60/month) + storage costs*

---
*Analysis based on AWS pricing for Asia Pacific (Mumbai) region as of March 2026*
