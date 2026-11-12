package org.contourgara.infrastructure;

import static org.assertj.core.api.Assertions.*;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.MountableFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.S3Object;

@SpringBootTest
class S3RepositoryImplTest {
    static final int PORT = 9297;
    static final String REGION = "ap-northeast-1";
    static final String ACCESS_KEY_ID = "test";
    static final String SECRET_KEY = "test";

    // Dockerfile を置かずに、compose.yml と同一のイメージをテストコード内で組み立てる。
    // テスト終了時にイメージを保持して次回テストでの RPM 再ダウンロードを防ぐため、第 2 引数を false とする。
    // 起動処理は compose と揃えるため、aws-emulator/start.sh を共用する。
    static GenericContainer<?> awsEmulator = new GenericContainer<>(
            new ImageFromDockerfile("spring-s3-sample-aws-emulator", false)
                    .withDockerfileFromBuilder(builder -> builder
                            .from("public.ecr.aws/amazonlinux/amazonlinux:2023-minimal")
                            .run("curl -fsSL -o /tmp/aws-emulator.rpm \"https://downloads.aws.com/aws-emulator/packages/aws-emulator/LTS/latest/aws-emulator-$(uname -m).rpm\""
                                    + " && rpm -i /tmp/aws-emulator.rpm"
                                    + " && rm /tmp/aws-emulator.rpm")
                            .build()))
            .withCopyFileToContainer(MountableFile.forHostPath("../aws-emulator/start.sh"), "/aws-emulator/start.sh")
            .withCommand("sh", "/aws-emulator/start.sh")
            .withExposedPorts(PORT)
            .waitingFor(Wait.forHttps("/__admin/health").allowInsecure());

    // 本番コードの HTTP クライアント設定を変更できないため、JVM 全体のトラストストアを差し替える。
    // 起動ごとに生成される自己署名証明書をコンテナから取り出して信頼させる。
    static {
        awsEmulator.start();
        String trustStore = awsEmulator.copyFileFromContainer(
                "/root/.aws/emulator/" + PORT + "/truststore.p12",
                inputStream -> {
                    Path path = Files.createTempFile("aws-emulator-truststore", ".p12");
                    Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
                    return path.toString();
                });
        System.setProperty("javax.net.ssl.trustStore", trustStore);
        System.setProperty("javax.net.ssl.trustStoreType", "PKCS12");
    }

    // 証明書の対象ホストに合わせつつ、SDK にパス形式を使わせるため 127.0.0.1 で接続する。
    // AWS Emulator は仮想ホスト形式に対応しないため、IP アドレスによる指定を必須とする。
    static String endpoint() {
        return "https://127.0.0.1:" + awsEmulator.getMappedPort(PORT);
    }

    @Autowired
    S3Repository sut;

    S3Client s3Client = S3Client.builder()
            .credentialsProvider(() -> AwsBasicCredentials.create(ACCESS_KEY_ID, SECRET_KEY))
            .region(Region.of(REGION))
            .endpointOverride(URI.create(endpoint()))
            .build();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("aws.access-key-id", () -> ACCESS_KEY_ID);
        registry.add("aws.secret-key", () -> SECRET_KEY);
        registry.add("aws.region", () -> REGION);
        registry.add("aws.s3.endpoint", () -> endpoint());
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
