package org.contourgara.domain;

import com.fasterxml.uuid.Generators;

public record PhotoNote(String id, String note) {
    public static PhotoNote create(String note) {
        return new PhotoNote(Generators.timeBasedEpochGenerator().generate().toString(), note);
    }
}
