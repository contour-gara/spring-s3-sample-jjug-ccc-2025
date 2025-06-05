package org.contourgara.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.contourgara.domain.PhotoNote;
import org.contourgara.domain.PhotoNoteRepository;
import org.contourgara.domain.S3Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class FindAllPhotoNoteUseCaseTest {
    @InjectMocks
    FindAllPhotoNoteUseCase sut;

    @Mock
    PhotoNoteRepository photoNoteRepository;
    @Mock
    S3Repository s3Repository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void 全件検索できる() {
        // setup
        doReturn(List.of(
                new PhotoNote("01973f92-b6a3-7a7a-bb15-97b7931a8bee", "test"),
                new PhotoNote("01973f9b-e8f9-74e6-a6c9-468ce9c1ca94", "test")
        )).when(photoNoteRepository).findAll();

        doReturn("http://localhost/1").when(s3Repository).createDownloadUrl("jjug-ccc-2025", "01973f92-b6a3-7a7a-bb15-97b7931a8bee");
        doReturn("http://localhost/2").when(s3Repository).createDownloadUrl("jjug-ccc-2025", "01973f9b-e8f9-74e6-a6c9-468ce9c1ca94");

        // execute
        List<FindAllPhotoNoteDto> actual = sut.execute();

        // assert
        List<FindAllPhotoNoteDto> expected = List.of(
                new FindAllPhotoNoteDto("01973f92-b6a3-7a7a-bb15-97b7931a8bee", "test", "http://localhost/1"),
                new FindAllPhotoNoteDto("01973f9b-e8f9-74e6-a6c9-468ce9c1ca94", "test", "http://localhost/2")
        );
        assertThat(actual).isEqualTo(expected);
    }
}
