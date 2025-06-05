package org.contourgara.domain;

import java.util.List;

public interface PhotoNoteRepository {
    List<PhotoNote> findAll();
    void save(PhotoNote photoNote);
}
