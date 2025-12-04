package main.back.utils.minio;

import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class MinioService {

    @Inject
    private MinioConfig minioConfig;

    public String uploadFile(InputStream fileStream, String fileName, String contentType, long size)
            throws Exception {
        try {
            minioConfig.getMinioClient().putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(fileName)
                            .stream(fileStream, size, -1)
                            .contentType(contentType)
                            .build());
            return fileName;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file to MinIO", e);
        }
    }

    public String getFileUrl(String fileName) throws Exception {
        Map<String, String> reqParams = new HashMap<>();
        reqParams.put("response-content-disposition", "attachment; filename=\"" + fileName + "\"");

        return minioConfig.getMinioClient().getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(minioConfig.getBucketName())
                        .object(fileName)
                        .expiry(7, TimeUnit.DAYS)
                        .extraQueryParams(reqParams)
                        .build());
    }

    public void deleteFile(String fileName) throws Exception {
        try {
            minioConfig.getMinioClient().removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(fileName)
                            .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete file from MinIO", e);
        }
    }

    public boolean fileExists(String fileName) throws Exception {
        try {
            minioConfig.getMinioClient().statObject(
                    StatObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(fileName)
                            .build());
            return true;
        } catch (ErrorResponseException e) {
            return false;
        }
    }

}