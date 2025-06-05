package org.contourgara.application;

import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.contourgara.domain.PhotoNote;
import org.contourgara.domain.PhotoNoteRepository;
import org.contourgara.domain.S3Repository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FindAllPhotoNoteUseCase {
    private final PhotoNoteRepository photoNoteRepository;
    private final S3Repository s3Repository;

    public List<FindAllPhotoNoteDto> execute() {
        List<PhotoNote> photoNotes = photoNoteRepository.findAll();
        List<String> urls = photoNotes.stream()
                .map(PhotoNote::id)
                .map(id -> s3Repository.createDownloadUrl("jjug-ccc-2025", id))
                .toList();

        return IntStream.range(0, photoNotes.size())
                .mapToObj(i -> new FindAllPhotoNoteDto(photoNotes.get(i).id(), photoNotes.get(i).note(), urls.get(i)))
                .toList();
    }
}
