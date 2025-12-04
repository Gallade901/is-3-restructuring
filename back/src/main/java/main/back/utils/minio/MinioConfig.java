// main.back.utils.minio.MinioConfig.java
package main.back.utils.minio;

import io.minio.MinioClient;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MinioConfig {

    private MinioClient minioClient;
    private final String bucketName = "import-files";

    @PostConstruct
    public void init() {
        try {
            this.minioClient = MinioClient.builder()
                    .endpoint("http://localhost:9000")
                    .credentials("minioadmin", "minioadmin")
                    .build();

            initializeBucket();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initializeBucket() {
        try {
            System.out.println("=== CHECKING BUCKET: " + bucketName + " ===");
            boolean found = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName)
                            .build()
            );

            if (!found) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucketName)
                                .build()
                );
            } else {
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error initializing MinIO bucket", e);
        }
    }

    public MinioClient getMinioClient() {
        return minioClient;
    }

    public String getBucketName() {
        return bucketName;
    }
}