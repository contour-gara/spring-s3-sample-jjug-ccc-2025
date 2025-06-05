package org.contourgara.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PhotoNoteTest {
    @Test
    void ノートからインスタンスを作成できる() {
        // execute
        PhotoNote actual = PhotoNote.create("test");

        // assert
        assertThat(actual).isInstanceOf(PhotoNote.class);
    }
}
