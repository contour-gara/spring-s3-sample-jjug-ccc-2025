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
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.S3Object;

@SpringBootTest
class S3RepositoryImplTest {
    static final String REGION = "ap-northeast-1";
    static final String ACCESS_KEY_ID = "testtest";
    static final String SECRET_KEY = "testtesttesttest";

    static final String GARAGE_CONFIG = """
            metadata_dir = "/var/lib/garage/meta"
            data_dir = "/var/lib/garage/data"
            db_engine = "sqlite"

            replication_factor = 1

            rpc_bind_addr = "[::]:3901"
            rpc_public_addr = "127.0.0.1:3901"
            rpc_secret = "9e3f2a6b1c8d4e7f0a5b2c9d6e3f8a1b4c7d0e5f2a9b6c3d8e1f4a7b0c5d2e9f"

            [s3_api]
            s3_region = "%s"
            api_bind_addr = "[::]:9000"
            root_domain = ".s3.localhost"

            [admin]
            api_bind_addr = "[::]:3903"
            """.formatted(REGION);

    static GenericContainer<?> garageContainer = new GenericContainer<>(DockerImageName.parse("dxflrs/garage:v2.3.0"))
            .withCopyToContainer(Transferable.of(GARAGE_CONFIG), "/etc/garage.toml")
            .withExposedPorts(9000, 3903)
            .waitingFor(Wait.forHttp("/health").forPort(3903).forStatusCode(200).forStatusCode(503));

    static {
        garageContainer.start();
        String nodeId = execInGarage("/garage", "node", "id", "-q").split("@")[0];
        execInGarage("/garage", "layout", "assign", "-z", "dc1", "-c", "1G", nodeId);
        execInGarage("/garage", "layout", "apply", "--version", "1");
        execInGarage("/garage", "key", "import", "--yes", "-n", "test", ACCESS_KEY_ID, SECRET_KEY);
        execInGarage("/garage", "key", "allow", "--create-bucket", ACCESS_KEY_ID);
    }

    static String execInGarage(String... command) {
        try {
            Container.ExecResult result = garageContainer.execInContainer(command);
            if (result.getExitCode() != 0) {
                throw new IllegalStateException("Garage のセットアップに失敗しました。: " + result.getStdout() + result.getStderr());
            }
            return result.getStdout().trim();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Autowired
    S3Repository sut;

    S3Client s3Client = S3Client.builder()
            .credentialsProvider(() -> AwsBasicCredentials.create(ACCESS_KEY_ID, SECRET_KEY))
            .region(Region.of(REGION))
            .endpointOverride(URI.create("http://s3.localhost:" + garageContainer.getMappedPort(9000)))
            .serviceConfiguration(S3Configuration.builder().chunkedEncodingEnabled(false).build())
            .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
            .build();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("aws.access-key-id", () -> ACCESS_KEY_ID);
        registry.add("aws.secret-key", () -> SECRET_KEY);
        registry.add("aws.region", () -> REGION);
        registry.add("aws.s3.endpoint", () -> "http://s3.localhost:" + garageContainer.getMappedPort(9000));
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
