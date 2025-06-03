package org.contourgara;

import static org.assertj.core.api.Assertions.*;

import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

@SpringBootTest
@Testcontainers
class S3RepositoryTest {
    @Container
    static LocalStackContainer localStackContainer = new LocalStackContainer(DockerImageName.parse("localstack/localstack:s3-latest"));

    @Autowired
    S3Repository sut;

    S3Client s3Client = S3Client.builder()
            .credentialsProvider(() -> AwsBasicCredentials.create(localStackContainer.getAccessKey(), localStackContainer.getSecretKey()))
            .region(Region.of(localStackContainer.getRegion()))
            .endpointOverride(URI.create(localStackContainer.getEndpoint().toString()))
            .build();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("aws.access-key-id", () -> localStackContainer.getAccessKey());
        registry.add("aws.secret-key", () -> localStackContainer.getSecretKey());
        registry.add("aws.region", () -> localStackContainer.getRegion());
        registry.add("aws.s3.endpoint", () -> localStackContainer.getEndpoint());
    }

    @BeforeEach
    void setUp() {
        for (String bucket : s3Client.listBuckets().buckets().stream().map(Bucket::name).toList()) {
            for (String key : s3Client.listObjectsV2(request -> request.bucket(bucket)).contents().stream().map(S3Object::key).toList()) {
                s3Client.deleteObject(request -> request.bucket(bucket).key(key));
            }
            s3Client.deleteBucket(request -> request.bucket(bucket));
        }
    }

    @Test
    void ダウンロード確認() {
        // setup
        s3Client.createBucket(createBucketRequest -> createBucketRequest.bucket("test-bucket"));
        s3Client.putObject(request -> request.bucket("test-bucket").key("test"), RequestBody.fromString("Hello S3!!!"));

        // execute
        String actual =  sut.download("test-bucket", "test");

        // assert
        assertThat(actual).isNotBlank();
    }

    @Test
    void アップロード確認() {
        // setup
        s3Client.createBucket(createBucketRequest -> createBucketRequest.bucket("test-bucket"));

        // execute
        String actual =  sut.upload("test-bucket", "test");

        // assert
        assertThat(actual).isNotBlank();
    }
}
