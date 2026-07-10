package com.nh.qagpt.dto;

import com.nh.qagpt.domain.Document;
import com.nh.qagpt.domain.Project;
import com.nh.qagpt.domain.ReviewResult;
import com.nh.qagpt.domain.enums.ArtifactType;
import com.nh.qagpt.domain.enums.Stage;

/**
 * 검토 현황 목록용 요약 DTO. 산출물 유형/파일명/프로젝트는 LAZY 연관(document→project)에서
 * 가져오므로 반드시 트랜잭션 내에서 {@link #from} 을 호출해야 한다(open-in-view=false).
 */
public record ReviewSummaryDto(
        Long reviewId,
        String artifactType,
        String artifactLabel,
        String fileName,
        Long projectId,
        String projectCode,
        String projectName,
        String stage,
        int round,
        String status,
        boolean passed,
        int defectCount,
        int improvementCount,
        int recommendationCount,
        String createdAt) {

    public static ReviewSummaryDto from(ReviewResult r, int defectCount,
                                        int improvementCount, int recommendationCount) {
        Document doc = r.getDocument();
        ArtifactType at = doc == null ? null : doc.getArtifactType();
        Project proj = doc == null ? null : doc.getProject();
        Stage st = r.getStage();
        return new ReviewSummaryDto(
                r.getId(),
                at == null ? null : at.name(),
                at == null ? null : at.getLabel(),
                doc == null ? null : doc.getFileName(),
                proj == null ? null : proj.getId(),
                proj == null ? null : proj.getCode(),
                proj == null ? null : proj.getName(),
                st == null ? null : st.getLabel(),
                r.getRound(),
                r.getStatus() == null ? null : r.getStatus().name(),
                r.isPassed(),
                defectCount,
                improvementCount,
                recommendationCount,
                r.getCreatedAt() == null ? null : r.getCreatedAt().toString());
    }
}
