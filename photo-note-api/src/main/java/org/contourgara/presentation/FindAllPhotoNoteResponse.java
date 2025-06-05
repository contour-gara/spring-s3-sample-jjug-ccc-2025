package org.contourgara.presentation;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record FindAllPhotoNoteResponse(
        @JsonProperty("photoNotes") List<FindPhotoNoteResponse> photoNoteResponses
) {
}
