package org.contourgara;

import static org.assertj.core.api.Assertions.*;

import java.net.URI;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;

@SpringBootTest
class S3RepositoryTest {
    static final String REGION = "ap-northeast-1";
    static final String ACCESS_KEY_ID = "testtest";
    static final String SECRET_KEY = "testtesttesttest";

    static GenericContainer<?> garageContainer = new GenericContainer<>(
            new ImageFromDockerfile().withFileFromPath(".", Path.of("../garage")))
            .withExposedPorts(3900, 3903)
            .waitingFor(Wait.forHttp("/health").forPort(3903).forStatusCode(200));

    static {
        garageContainer.start();
    }

    @Autowired
    S3Repository sut;

    S3Client s3Client = S3Client.builder()
            .credentialsProvider(() -> AwsBasicCredentials.create(ACCESS_KEY_ID, SECRET_KEY))
            .region(Region.of(REGION))
            .endpointOverride(URI.create("http://s3.localhost:" + garageContainer.getMappedPort(3900)))
            .serviceConfiguration(S3Configuration.builder().chunkedEncodingEnabled(false).build())
            .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
            .build();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("aws.access-key-id", () -> ACCESS_KEY_ID);
        registry.add("aws.secret-key", () -> SECRET_KEY);
        registry.add("aws.region", () -> REGION);
        registry.add("aws.s3.endpoint", () -> "http://s3.localhost:" + garageContainer.getMappedPort(3900));
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
