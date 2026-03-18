#!/bin/bash
# Install Mountpoint for S3, Amazon Corretto, and Maven on EC2 workstation (Amazon Linux 2023)

set -e

CORRETTO_VERSION="${1:-21}"

echo "Installing Mountpoint for S3, Amazon Corretto $CORRETTO_VERSION, and Maven..."

# Install Mountpoint (included in AL2023)
sudo dnf install -y mountpoint-s3

# Install Amazon Corretto
sudo dnf install -y java-${CORRETTO_VERSION}-amazon-corretto

# Install Maven
sudo dnf install -y maven

echo "Installation complete"

# Verify installations
echo "Mountpoint version:"
mount-s3 --version

echo "Java version:"
java -version

echo "Maven version:"
mvn --version
