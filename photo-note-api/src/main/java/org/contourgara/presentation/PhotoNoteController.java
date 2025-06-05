package org.contourgara.presentation;

import lombok.RequiredArgsConstructor;
import org.contourgara.application.FindAllPhotoNoteUseCase;
import org.contourgara.application.SavePhotoUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@CrossOrigin
public class PhotoNoteController {
    private final FindAllPhotoNoteUseCase findAllPhotoNoteUseCase;
    private final SavePhotoUseCase savePhotoUseCase;

    @GetMapping("/photonote")
    @ResponseStatus(HttpStatus.OK)
    public FindAllPhotoNoteResponse findAllPhotoNote() {
        return new FindAllPhotoNoteResponse(
                findAllPhotoNoteUseCase.execute().stream()
                        .map(dto -> new FindPhotoNoteResponse(dto.note(), dto.url()))
                        .toList()
        );
    }

    @PostMapping("/photonote")
    @ResponseStatus(HttpStatus.CREATED)
    public SavePhotoNoteResponse savePhotoNote(@RequestBody SavePhotoNoteRequest photoNoteRequest) {
        return new SavePhotoNoteResponse(savePhotoUseCase.execute(photoNoteRequest.note()));
    }
}
