package org.contourgara.presentation;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import java.util.List;
import org.contourgara.application.FindAllPhotoNoteDto;
import org.contourgara.application.FindAllPhotoNoteUseCase;
import org.contourgara.application.SavePhotoUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PhotoNoteController.class)
class PhotoNoteControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    FindAllPhotoNoteUseCase findAllPhotoNoteUseCase;
    @MockitoBean
    SavePhotoUseCase savePhotoUseCase;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @Test
    void 全件検索() {
        // setup
        doReturn(List.of(
                new FindAllPhotoNoteDto("1", "test1", "http://localhost/1"),
                new FindAllPhotoNoteDto("2", "test2", "http://localhost/2")
        )).when(findAllPhotoNoteUseCase).execute();

        // execute & assert
        given()
                .when()
                .get("/photonote")
                .then()
                .statusCode(200)
                .header("Content-Type", "application/json")
                .body("photoNotes[0].note", equalTo("test1"))
                .body("photoNotes[0].url", equalTo("http://localhost/1"))
                .body("photoNotes[1].note", equalTo("test2"))
                .body("photoNotes[1].url", equalTo("http://localhost/2"));
    }

    @Test
    void 保存() {
        // setup
        doReturn("http://localhost/1").when(savePhotoUseCase).execute("test");

        // execute & assert
        given()
                .header("Content-Type", "application/json")
                .body(new SavePhotoNoteRequest("test"))
                .when()
                .post("/photonote")
                .then()
                .statusCode(201)
                .header("Content-Type", "application/json")
                .body("url", equalTo("http://localhost/1"));
    }
}
