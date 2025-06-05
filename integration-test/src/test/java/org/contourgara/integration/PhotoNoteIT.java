package org.contourgara.integration;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

import com.github.database.rider.core.api.configuration.DBUnit;
import com.github.database.rider.core.api.configuration.Orthography;
import com.github.database.rider.core.api.connection.ConnectionHolder;
import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import com.github.database.rider.junit5.api.DBRider;
import java.net.URI;
import java.sql.DriverManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.S3Object;

@DBUnit(caseInsensitiveStrategy = Orthography.LOWERCASE)
@DBRider
class PhotoNoteIT {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/jjug";
    private static final String DB_USER = "gara";
    private static final String DB_PASSWORD = "password";

    private static final ConnectionHolder connectionHolder =
            () -> DriverManager.getConnection(
                    DB_URL,
                    DB_USER,
                    DB_PASSWORD
            );

    private static final String AWS_ACCESS_KEY_ID = "test";
    private static final String AWS_SECRET_KEY = "test123456";
    private static final String AWS_REGION = "ap-northeast-1";
    private static final String AWS_S3_ENDPOINT = "http://localhost:4566/";

    S3Client s3Client = S3Client.builder()
            .credentialsProvider(() -> AwsBasicCredentials.create(AWS_ACCESS_KEY_ID, AWS_SECRET_KEY))
            .region(Region.of(AWS_REGION))
            .endpointOverride(URI.create(AWS_S3_ENDPOINT))
            .forcePathStyle(true)
            .build();

    @BeforeAll
    static void setUpAll() {
        baseURI = "http://localhost:8080";
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
    @DataSet("datasets/setup/photo-note-2.yml")
    @ExpectedDataSet("datasets/expected/photo-note-2.yml")
    void 全件検索した場合ノートとURLが返る() {
        // setup
        s3Client.createBucket(createBucketRequest -> createBucketRequest.bucket("jjug-ccc-2025"));
        s3Client.putObject(request -> request.bucket("jjug-ccc-2025").key("01973f92-b6a3-7a7a-bb15-97b7931a8bee"), RequestBody.fromString(""));
        s3Client.putObject(request -> request.bucket("jjug-ccc-2025").key("01973f9b-e8f9-74e6-a6c9-468ce9c1ca94"), RequestBody.fromString(""));

        // execute & assert
        given()
                .get("/photonote")
                .then()
                .statusCode(200)
                .header("Content-Type", "application/json")
                .body("photoNotes[0].note", equalTo("test1"))
                .body("photoNotes[0].url", startsWith("http://jjug-ccc-2025.localstack:4566/"))
                .body("photoNotes[1].note", equalTo("test2"))
                .body("photoNotes[1].url", startsWith("http://jjug-ccc-2025.localstack:4566/"));
    }

    @Test
    @DataSet("datasets/setup/photo-note-0.yml")
    @ExpectedDataSet("datasets/expected/photo-note-1.yml")
    void 保存の場合ノートがDBに保存されURLが返る() {
        // setup
        s3Client.createBucket(createBucketRequest -> createBucketRequest.bucket("jjug-ccc-2025"));

        // execute & assert
        given()
                .header("Content-Type", "application/json")
                .body("""
                        {
                        "note": "test"
                        }
                        """)
                .when()
                .post("/photonote")
                .then()
                .statusCode(201)
                .header("Content-Type", "application/json")
                .body("url", startsWith("http://jjug-ccc-2025.localstack:4566/"));
    }
}
