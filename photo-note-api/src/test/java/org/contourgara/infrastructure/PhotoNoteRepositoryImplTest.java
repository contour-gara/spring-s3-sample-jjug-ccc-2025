package org.contourgara.infrastructure;

import static org.assertj.core.api.Assertions.*;

import com.github.database.rider.core.api.configuration.DBUnit;
import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import com.github.database.rider.junit5.api.DBRider;
import java.util.List;
import org.contourgara.domain.PhotoNote;
import org.contourgara.domain.PhotoNoteRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@DBUnit
@DBRider
class PhotoNoteRepositoryImplTest {
    @Autowired
    PhotoNoteRepository sut;

    @Nested
    class 全件検索 {
        @Test
        @DataSet("datasets/setup/photo-note-1.yml")
        @ExpectedDataSet("datasets/expected/photo-note-1.yml")
        void レコードが1件の場合() {
            // execute
            List<PhotoNote> actual = sut.findAll();

            // assert
            List<PhotoNote> expected = List.of(new PhotoNote("01973f92-b6a3-7a7a-bb15-97b7931a8bee", "test"));
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        @DataSet("datasets/setup/photo-note-2.yml")
        @ExpectedDataSet("datasets/expected/photo-note-2.yml")
        void レコードが2件の場合() {
            // execute
            List<PhotoNote> actual = sut.findAll();

            // assert
            List<PhotoNote> expected = List.of(
                    new PhotoNote("01973f92-b6a3-7a7a-bb15-97b7931a8bee", "test"),
                    new PhotoNote("01973f9b-e8f9-74e6-a6c9-468ce9c1ca94", "test")
            );
            assertThat(actual).isEqualTo(expected);
        }
    }

    @Test
    @DataSet("datasets/setup/photo-note-0.yml")
    @ExpectedDataSet("datasets/expected/photo-note-1.yml")
    void IDとノートを保存できる() {
        // setup
        PhotoNote photoNote = new PhotoNote("01973f92-b6a3-7a7a-bb15-97b7931a8bee", "test");

        // execute
        sut.save(photoNote);
    }
}
