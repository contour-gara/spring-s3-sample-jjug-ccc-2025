package org.contourgara.presentation;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SavePhotoNoteRequest(
        @JsonProperty("note") String note
) {
}
