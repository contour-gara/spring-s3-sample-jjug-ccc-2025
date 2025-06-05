package org.contourgara.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.contourgara.domain.PhotoNote;
import org.contourgara.domain.PhotoNoteRepository;
import org.contourgara.domain.S3Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class SavePhotoUseCaseTest {
    @InjectMocks
    SavePhotoUseCase sut;

    @Mock
    PhotoNoteRepository photoNoteRepository;
    @Mock
    S3Repository s3Repository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void DB保存とアップロード用のURLを作成できる() {
        // setup
        doReturn("http://localhost/1").when(s3Repository).createUploadUrl(eq("jjug-ccc-2025"), anyString());

        // execute
        String actual = sut.execute("test");

        // assert
        String expected = "http://localhost/1";
        assertThat(actual).isEqualTo(expected);

        verify(photoNoteRepository, times(1)).save(any(PhotoNote.class));
    }
}
