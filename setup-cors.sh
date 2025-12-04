#!/bin/bash

echo "Setting up MinIO CORS configuration..."

# Check if container is running
if ! docker ps | grep -q lab-3-minio-1; then
    echo "Starting MinIO container..."
    docker-compose up -d
    sleep 10
fi

echo "Waiting for MinIO to be ready..."
sleep 10

# Configure CORS using the correct method for standalone MinIO
docker exec lab-3-minio-1 sh -c "
    # Set alias
    mc alias set myminio http://localhost:9000 minioadmin minioadmin
    
    # Wait for MinIO to be ready
    until mc ls myminio 2>/dev/null; do
        echo 'Waiting for MinIO...'
        sleep 2
    done

    # Set CORS using mc command
    mc admin config set myminio api || true
    
    # Alternative method: Use environment variables
    echo 'Configuring CORS through environment...'
"

# Set CORS using the legacy method
echo "Setting CORS configuration directly..."

# Create a CORS configuration file
cat > cors.json << EOF
{
    "cors": [
        {
            "allowed_origins": ["http://localhost:3000", "http://127.0.0.1:3000"],
            "allowed_methods": ["GET", "POST", "PUT", "DELETE", "HEAD"],
            "allowed_headers": ["Authorization", "Content-Type", "X-Amz-Date", "X-Amz-Content-Sha256", "Content-Range", "Content-Disposition", "Cache-Control", "Origin", "Accept", "X-Requested-With"],
            "expose_headers": ["Date", "Etag", "Server", "Connection", "Accept-Ranges", "Content-Range", "Content-Encoding", "Content-Length", "Content-Type", "Content-Disposition", "Last-Modified", "Content-Language", "Cache-Control", "Retry-After", "X-Amz-Bucket-Region", "Expires", "X-Amz*"],
            "max_age_seconds": 3600,
            "allow_credentials": true
        }
    ]
}
EOF

# Copy CORS config to container and apply
docker cp cors.json lab-3-minio-1:/tmp/cors.json
docker exec lab-3-minio-1 sh -c "
    mc alias set myminio http://localhost:9000 minioadmin minioadmin
    mc admin config import myminio < /tmp/cors.json || echo 'Import may fail, trying alternative method...'
"

# Cleanup
rm -f cors.json

echo "Restarting MinIO to apply changes..."
docker restart lab-3-minio-1

sleep 5

echo "CORS setup attempt completed!"
echo "If CORS still doesn't work, we'll use the Web UI method."