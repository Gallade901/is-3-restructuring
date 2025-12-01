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
            System.out.println("=== INITIALIZING MINIO CLIENT ===");
            this.minioClient = MinioClient.builder()
                    .endpoint("http://localhost:9000")
                    .credentials("minioadmin", "minioadmin")
                    .build();

            initializeBucket();
            System.out.println("=== MINIO CLIENT INITIALIZED SUCCESSFULLY ===");
        } catch (Exception e) {
            System.err.println("=== ERROR INITIALIZING MINIO: " + e.getMessage() + " ===");
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
                System.out.println("=== BUCKET '" + bucketName + "' CREATED SUCCESSFULLY ===");
            } else {
                System.out.println("=== BUCKET '" + bucketName + "' ALREADY EXISTS ===");
            }
        } catch (Exception e) {
            System.err.println("=== ERROR INITIALIZING BUCKET: " + e.getMessage() + " ===");
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