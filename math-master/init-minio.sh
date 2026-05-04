#!/bin/sh
#
# init-minio.sh — bootstrap MinIO buckets used by the platform.
#
# Each bucket needs three things to be reachable from a browser via a presigned
# URL:
#   1. The bucket must exist.
#   2. CORS must be configured so the preflight succeeds.
#   3. A public-read (anonymous download) policy so the GET inside the URL is
#      allowed by MinIO's policy engine. (The signed URL still scopes the GET to
#      the keys we hand out.)
#
# The pre-existing version of this script provisioned only `course-videos`. The
# `course-materials` bucket was created lazily on first upload, ended up without
# CORS or a download policy, and silently broke downloads for every role —
# students, teachers, and admins all hit a CORS error after we issued a valid
# presigned URL. This script now bootstraps every bucket the same way.

set -e

echo "Waiting for MinIO to be ready..."
until curl -sf http://minio:9000/minio/health/live > /dev/null 2>&1; do
  echo "MinIO is not ready yet. Waiting..."
  sleep 2
done

echo "MinIO is ready. Configuring..."

mc alias set myminio http://minio:9000 minioadmin minioadmin

# All buckets that browsers will fetch via presigned URLs must be created up
# front so we can attach CORS + the download policy. Add new buckets here.
buckets="course-videos course-materials slide-templates teacher-verifications"

for b in $buckets; do
  echo "Provisioning bucket: $b"
  mc mb "myminio/$b" --ignore-existing
  mc anonymous set download "myminio/$b"
  mc cors set /cors-config.xml "myminio/$b"
done

echo "MinIO configuration completed!"
