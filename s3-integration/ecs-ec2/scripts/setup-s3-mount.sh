#!/bin/bash
# Mount S3 bucket using Mountpoint for S3

set -e

BUCKET_NAME="${1:-}"
MOUNT_POINT="${2:-/mnt/s3}"

if [ -z "$BUCKET_NAME" ]; then
    echo "Usage: $0 <bucket-name> [mount-point]"
    echo "Example: $0 my-bucket /mnt/s3"
    exit 1
fi

echo "Mounting S3 bucket $BUCKET_NAME to $MOUNT_POINT..."

# Create mount point if it doesn't exist
sudo mkdir -p "$MOUNT_POINT"

# Mount S3 bucket
sudo mount-s3 "$BUCKET_NAME" "$MOUNT_POINT" \
    --allow-delete \
    --allow-overwrite \
    --allow-other \
    --uid 1000 \
    --gid 1000

echo "S3 bucket mounted successfully"
