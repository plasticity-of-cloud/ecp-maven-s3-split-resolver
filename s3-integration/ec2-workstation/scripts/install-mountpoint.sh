#!/bin/bash
# Install Mountpoint for S3 on EC2 workstation (Amazon Linux 2023)

set -e

echo "Installing Mountpoint for S3..."

# Install Mountpoint (included in AL2023)
sudo dnf install -y mountpoint-s3

echo "Mountpoint installed successfully"

# Verify installation
mount-s3 --version
