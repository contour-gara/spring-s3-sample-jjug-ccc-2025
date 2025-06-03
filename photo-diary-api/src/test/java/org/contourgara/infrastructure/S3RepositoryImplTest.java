package org.contourgara.infrastructure;

import static org.assertj.core.api.Assertions.*;

import java.net.URI;
import org.contourgara.domain.S3Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.S3Object;

@SpringBootTest
@Testcontainers
class S3RepositoryImplTest {
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

    @Nested
    class アップロード用の署名付きURL取得テスト {
        @ParameterizedTest
        @CsvSource({
                "test-bucket, test",
                "abc, d"
        })
        void バケットが存在しオブジェクトが存在しない場合署名付きURLが返る(String bucket, String key) {
            // setup
            s3Client.createBucket(createBucketRequest -> createBucketRequest.bucket(bucket));

            // execute
            String actual =  sut.createUploadUrl(bucket, key);

            // assert
            assertThat(actual).isNotBlank();
        }

        @Test
        void バケットが存在しない場合例外が返る () {
            // execute & assert
            assertThatThrownBy(() -> sut.createUploadUrl("test-bucket", "test"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("バケットが存在しません。");
        }

        @Test
        void オブジェクトが存在する場合例外が返る () {
            // setup
            s3Client.createBucket(createBucketRequest -> createBucketRequest.bucket("test-bucket"));
            s3Client.putObject(request -> request.bucket("test-bucket").key("test"), RequestBody.fromString(""));

            // execute & assert
            assertThatThrownBy(() -> sut.createUploadUrl("test-bucket", "test"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("オブジェクトが既に存在します。");
        }
    }

    @Nested
    class ダウンロード用の署名付きURL取得テスト {
        @ParameterizedTest
        @CsvSource({
                "test-bucket, test",
                "abc, d"
        })
        void バケットとブジェクトが存在する場合署名付きURLが返る(String bucket, String key) {
            // setup
            s3Client.createBucket(createBucketRequest -> createBucketRequest.bucket(bucket));
            s3Client.putObject(request -> request.bucket(bucket).key(key), RequestBody.fromString(""));

            // execute
            String actual =  sut.createDownloadUrl(bucket, key);

            // assert
            assertThat(actual).isNotBlank();
        }

        @Test
        void バケットが存在しない場合例外が返る () {
            // execute & assert
            assertThatThrownBy(() -> sut.createDownloadUrl("test-bucket", "test"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("バケットが存在しません。");
        }

        @Test
        void オブジェクトが存在しない場合例外が返る () {
            // setup
            s3Client.createBucket(createBucketRequest -> createBucketRequest.bucket("test-bucket"));

            // execute & assert
            assertThatThrownBy(() -> sut.createDownloadUrl("test-bucket", "test"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("オブジェクトが存在しません。");
        }
    }
}
