#!/bin/bash
# Install Maven S3 Split Resolver on EC2 workstation

set -e

VERSION="${1:-1.0.0}"
JAR_URL="${2:-https://github.com/plasticity-of-cloud/ecp-maven-s3-split-resolver/releases/download/v${VERSION}/maven-s3-split-resolver.jar}"

echo "Installing Maven S3 Split Resolver v${VERSION}..."

# Download JAR
curl -L "$JAR_URL" -o /tmp/maven-s3-split-resolver.jar

# Create Maven extensions directory if it doesn't exist
MAVEN_HOME="${MAVEN_HOME:-/usr/share/maven}"
EXTENSIONS_DIR="${MAVEN_HOME}/lib/ext"

if [ ! -d "$EXTENSIONS_DIR" ]; then
    echo "Creating extensions directory: $EXTENSIONS_DIR"
    sudo mkdir -p "$EXTENSIONS_DIR"
fi

# Copy JAR to Maven extensions directory
sudo cp /tmp/maven-s3-split-resolver.jar "$EXTENSIONS_DIR/"

# Clean up
rm /tmp/maven-s3-split-resolver.jar

echo "JAR installed to $EXTENSIONS_DIR"

# Configure MAVEN_OPTS for ec2-user
echo "Configuring MAVEN_OPTS for ec2-user..."

# Add to .bashrc if not already present
if ! grep -q "s3.resolver.artifactDir" /home/ec2-user/.bashrc 2>/dev/null; then
    cat << 'EOF' | sudo tee -a /home/ec2-user/.bashrc

# Maven S3 Split Resolver configuration
export MAVEN_OPTS="-Dmaven.repo.local=/home/ec2-user/.m2-metadata/repository-metadata -Ds3.resolver.artifactDir=/home/ec2-user/.m2/repository"
EOF
    echo "MAVEN_OPTS added to /home/ec2-user/.bashrc"
else
    echo "MAVEN_OPTS already configured in /home/ec2-user/.bashrc"
fi

echo "Installation complete!"
