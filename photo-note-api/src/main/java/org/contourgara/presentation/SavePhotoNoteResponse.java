package org.contourgara.presentation;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SavePhotoNoteResponse(
        @JsonProperty("url") String url
) {
}
