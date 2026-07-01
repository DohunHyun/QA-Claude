package com.nh.qagpt.dto;

import com.nh.qagpt.domain.Document;

public record UploadResponse(Long documentId, String fileName, String artifactType) {

    public static UploadResponse from(Document d) {
        return new UploadResponse(
                d.getId(), d.getFileName(),
                d.getArtifactType() == null ? null : d.getArtifactType().name());
    }
}
