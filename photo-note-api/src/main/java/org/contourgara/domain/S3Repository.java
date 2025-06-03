package org.contourgara.domain;

public interface S3Repository {
    String createUploadUrl(String bucket, String key);
    String createDownloadUrl(String bucket, String key);
}
