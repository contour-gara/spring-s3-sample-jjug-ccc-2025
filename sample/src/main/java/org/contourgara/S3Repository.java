package org.contourgara;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Repository
public class S3Repository {
    private final S3Presigner s3Presigner;
    private final S3Client s3Client;

    public S3Repository(AwsConfig awsConfig) {
        if (StringUtils.isEmpty(awsConfig.getS3().getEndpoint())) {
            s3Presigner = S3Presigner.builder()
                    .credentialsProvider(() -> AwsBasicCredentials.create(awsConfig.getAccessKeyId(), awsConfig.getSecretKey()))
                    .region(Region.of(awsConfig.getRegion()))
                    .build();

            s3Client = S3Client.builder()
                    .credentialsProvider(() -> AwsBasicCredentials.create(awsConfig.getAccessKeyId(), awsConfig.getSecretKey()))
                    .region(Region.of(awsConfig.getRegion()))
                    .build();
        } else {
            s3Presigner = S3Presigner.builder()
                    .credentialsProvider(() -> AwsBasicCredentials.create(awsConfig.getAccessKeyId(), awsConfig.getSecretKey()))
                    .region(Region.of(awsConfig.getRegion()))
                    .endpointOverride(URI.create(awsConfig.getS3().getEndpoint()))
                    .build();

            s3Client = S3Client.builder()
                    .credentialsProvider(() -> AwsBasicCredentials.create(awsConfig.getAccessKeyId(), awsConfig.getSecretKey()))
                    .region(Region.of(awsConfig.getRegion()))
                    .endpointOverride(URI.create(awsConfig.getS3().getEndpoint()))
                    .build();
        }
    }

    public String upload(String bucket, String key) {
        if (notExistBucket(bucket)) throw new RuntimeException("バケットが存在しません。");

        return s3Presigner
                .presignPutObject(
                        putObjectPresignRequest -> putObjectPresignRequest
                                .putObjectRequest(putObjectRequest -> putObjectRequest.bucket(bucket).key(key))
                                .signatureDuration(Duration.ofMinutes(10))
                )
                .url()
                .toExternalForm();
    }

    public String download(String bucket, String key) {
        if (notExistBucket(bucket)) throw new RuntimeException("バケットが存在しません。");
        if (notExistObject(bucket, key)) throw new RuntimeException("オブジェクトが存在しません。");

        return s3Presigner
                .presignGetObject(
                        getObjectPresignRequest -> getObjectPresignRequest
                                .getObjectRequest(getObjectRequest -> getObjectRequest.bucket(bucket).key(key))
                                .signatureDuration(Duration.ofMinutes(10))
                )
                .url()
                .toExternalForm();
    }

    private boolean notExistBucket(String bucket) {
        List<String> buckets = s3Client.listBuckets().buckets().stream()
                .map(Bucket::name)
                .toList();

        return !buckets.contains(bucket);
    }

    private boolean notExistObject(String bucket, String key) {
        List<String> objects = s3Client.listObjectsV2(request -> request.bucket(bucket)).contents().stream()
                .map(S3Object::key)
                .toList();

        return !objects.contains(key);
    }
}
