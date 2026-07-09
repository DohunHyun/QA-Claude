package com.nh.qagpt.dto;

import com.nh.qagpt.domain.Document;
import com.nh.qagpt.domain.ReviewResult;
import com.nh.qagpt.domain.enums.ArtifactType;

/**
 * 검토 현황 목록용 요약 DTO. 산출물 유형/파일명은 LAZY 연관(document)에서 가져오므로
 * 반드시 트랜잭션 내에서 {@link #from} 을 호출해야 한다(open-in-view=false).
 */
public record ReviewSummaryDto(
        Long reviewId,
        String artifactType,
        String artifactLabel,
        String fileName,
        int round,
        String status,
        boolean passed,
        int defectCount,
        String createdAt) {

    public static ReviewSummaryDto from(ReviewResult r, int defectCount) {
        Document doc = r.getDocument();
        ArtifactType at = doc == null ? null : doc.getArtifactType();
        return new ReviewSummaryDto(
                r.getId(),
                at == null ? null : at.name(),
                at == null ? null : at.getLabel(),
                doc == null ? null : doc.getFileName(),
                r.getRound(),
                r.getStatus() == null ? null : r.getStatus().name(),
                r.isPassed(),
                defectCount,
                r.getCreatedAt() == null ? null : r.getCreatedAt().toString());
    }
}
