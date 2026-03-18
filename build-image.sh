#!/bin/bash
# Copyright © 2026 Plasticity.Cloud and CoDriverLabs. All rights reserved.
set -e

# Parse arguments
REGION=""
while [[ $# -gt 0 ]]; do
  case $1 in
    --region)
      REGION="$2"
      shift 2
      ;;
    *)
      echo "Unknown option: $1"
      echo "Usage: $0 [--region REGION]"
      exit 1
      ;;
  esac
done

# Get AWS account ID
AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
if [ -z "$AWS_ACCOUNT_ID" ]; then
  echo "Error: Could not determine AWS account ID"
  exit 1
fi

# Get region from AWS config if not provided
if [ -z "$REGION" ]; then
  REGION=$(aws configure get region)
  if [ -z "$REGION" ]; then
    echo "Error: No region specified and no default region configured"
    exit 1
  fi
fi

echo "Building for account: $AWS_ACCOUNT_ID, region: $REGION"

# Build the JAR
mvn clean package

# Authenticate to public ECR
aws ecr-public get-login-password --region us-east-1 | docker login --username AWS --password-stdin public.ecr.aws

# Authenticate to private ECR
aws ecr get-login-password --region "$REGION" | docker login --username AWS --password-stdin "$AWS_ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com"

# Build image
IMAGE_NAME="$AWS_ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com/docker/library/maven:3.9-amazoncorretto-21-al2023-s3"
docker build -t "$IMAGE_NAME" .

# Push image
docker push "$IMAGE_NAME"

echo "Successfully built and pushed: $IMAGE_NAME"
