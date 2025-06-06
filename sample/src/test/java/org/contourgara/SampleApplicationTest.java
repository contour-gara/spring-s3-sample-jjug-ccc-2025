package org.contourgara;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SampleApplicationTest {
    @Autowired
    AwsConfig awsConfig;

    @Test
    void contextLoads() {
        assertThat(awsConfig).isNotNull();
    }
}
