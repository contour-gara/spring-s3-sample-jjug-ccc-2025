package org.contourgara.presentation;

import lombok.RequiredArgsConstructor;
import org.contourgara.domain.S3Repository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TestController {
    private final S3Repository s3Repository;

    @GetMapping("/test")
    @ResponseStatus(HttpStatus.OK)
    public String test() {
        return s3Repository.createUploadUrl("test-api3", "test");
    }
}
