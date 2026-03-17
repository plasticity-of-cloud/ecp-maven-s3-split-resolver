#!/usr/bin/env python3

import yaml
import boto3
import sys
from pathlib import Path

def clear_s3_bucket():
    # Read bucket name from Helm values.yaml
    values_path = Path(__file__).parent / 'helm-chart' / 'values.yaml'
    
    with open(values_path, 'r') as file:
        values = yaml.safe_load(file)
    
    bucket_name = values.get('s3', {}).get('bucketName')
    
    if not bucket_name:
        print("Error: Could not find s3.bucketName in values.yaml")
        sys.exit(1)
    
    print(f"Clearing S3 bucket: {bucket_name}")
    
    # Initialize S3 client
    s3 = boto3.client('s3')
    
    # Delete all objects
    paginator = s3.get_paginator('list_objects_v2')
    for page in paginator.paginate(Bucket=bucket_name):
        if 'Contents' in page:
            objects = [{'Key': obj['Key']} for obj in page['Contents']]
            s3.delete_objects(Bucket=bucket_name, Delete={'Objects': objects})
            print(f"Deleted {len(objects)} objects")
    
    # Delete all versions (if versioning enabled)
    paginator = s3.get_paginator('list_object_versions')
    for page in paginator.paginate(Bucket=bucket_name):
        versions = []
        if 'Versions' in page:
            versions.extend([{'Key': v['Key'], 'VersionId': v['VersionId']} for v in page['Versions']])
        if 'DeleteMarkers' in page:
            versions.extend([{'Key': d['Key'], 'VersionId': d['VersionId']} for d in page['DeleteMarkers']])
        
        if versions:
            s3.delete_objects(Bucket=bucket_name, Delete={'Objects': versions})
            print(f"Deleted {len(versions)} versions/markers")
    
    print(f"Successfully cleared bucket: {bucket_name}")

if __name__ == "__main__":
    clear_s3_bucket()
