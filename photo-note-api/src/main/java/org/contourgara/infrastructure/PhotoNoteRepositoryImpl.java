package org.contourgara.infrastructure;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.contourgara.domain.PhotoNote;
import org.contourgara.domain.PhotoNoteRepository;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PhotoNoteRepositoryImpl implements PhotoNoteRepository {
    private final JdbcClient jdbcClient;

    @Override
    public List<PhotoNote> findAll() {
        return jdbcClient.sql("SELECT id, note FROM photo_note")
                .query(new DataClassRowMapper<>(PhotoNote.class))
                .list();
    }

    @Override
    public void save(PhotoNote photoNote) {
        jdbcClient.sql("INSERT INTO photo_note (id, note) VALUES (?, ?)")
                .param(photoNote.id())
                .param(photoNote.note())
                .update();
    }
}
