package org.contourgara.application;

import lombok.RequiredArgsConstructor;
import org.contourgara.domain.PhotoNote;
import org.contourgara.domain.PhotoNoteRepository;
import org.contourgara.domain.S3Repository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SavePhotoUseCase {
    private final PhotoNoteRepository photoNoteRepository;
    private final S3Repository s3Repository;

    public String execute(String note) {
        PhotoNote photoNote = PhotoNote.create(note);
        photoNoteRepository.save(photoNote);
        return s3Repository.createUploadUrl("jjug-ccc-2025", photoNote.id());
    }
}
