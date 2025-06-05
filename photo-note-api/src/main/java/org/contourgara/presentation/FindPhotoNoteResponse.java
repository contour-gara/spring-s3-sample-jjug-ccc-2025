package org.contourgara.presentation;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FindPhotoNoteResponse(
        @JsonProperty("note") String note,
        @JsonProperty("url") String url
) {
}
