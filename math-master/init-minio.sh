#!/bin/sh

# Wait for MinIO to be ready
echo "Waiting for MinIO to be ready..."
until curl -sf http://minio:9000/minio/health/live > /dev/null 2>&1; do
  echo "MinIO is not ready yet. Waiting..."
  sleep 2
done

echo "MinIO is ready. Configuring..."

# Configure mc (MinIO Client)
mc alias set myminio http://minio:9000 minioadmin minioadmin

# Create buckets if they don't exist
mc mb myminio/course-videos --ignore-existing
mc mb myminio/slide-templates --ignore-existing
mc mb myminio/teacher-verifications --ignore-existing

# Set public read policy for course-videos (for presigned URLs to work)
mc anonymous set download myminio/course-videos

# Apply CORS configuration
mc cors set /cors-config.xml myminio/course-videos

echo "MinIO configuration completed!"
