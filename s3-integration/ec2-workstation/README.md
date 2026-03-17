# EC2 Workstation Setup Guide

## Prerequisites

- Amazon Linux 2023 EC2 instance
- AWS CLI configured with appropriate credentials

## Installation

```bash
# Install Mountpoint for S3 (included in AL2023)
./install-mountpoint.sh

# Mount your S3 bucket
sudo mount-s3 <your-bucket-name> /mnt/s3 --allow-delete --allow-overwrite
```

## Usage

After mounting, you can access S3 objects as files:

```bash
ls /mnt/s3/
cat /mnt/s3/path/to/file.txt
```

## Unmount

```bash
sudo umount /mnt/s3
```
