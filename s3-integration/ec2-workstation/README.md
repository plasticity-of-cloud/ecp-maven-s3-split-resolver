# EC2 Workstation Setup Guide

## Prerequisites

- Amazon Linux 2023 EC2 instance
- AWS CLI configured with appropriate credentials

## Installation

### Quick Install (All-in-One)

```bash
# Install Mountpoint, Corretto (default: 21), and Maven
./install-all.sh

# Or specify Corretto version
./install-all.sh 17
```

### Manual Install

```bash
# Install Mountpoint for S3 (included in AL2023)
./install-mountpoint.sh

# Install Amazon Corretto
sudo dnf install -y java-21-amazon-corretto

# Install Maven
sudo dnf install -y maven
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
