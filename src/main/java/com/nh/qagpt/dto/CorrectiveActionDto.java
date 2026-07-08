package com.nh.qagpt.dto;

import com.nh.qagpt.domain.CorrectiveAction;

/** [spec §4.4] 시정조치 라인 — 상태 추적용 응답. */
public record CorrectiveActionDto(
        Long id,
        String no,
        String businessName,
        String improvementType,   // 개선/권고
        String defectType,
        String artifactName,
        String fileName,
        String nonconformityLocation,
        String nonconformityContent,
        String actionPlan,
        String assignee,
        String plannedDate,
        String status,            // TARGET/IN_PROGRESS/DONE
        String confirmation,
        String confirmedDate
) {
    public static CorrectiveActionDto from(CorrectiveAction a) {
        return new CorrectiveActionDto(
                a.getId(), a.getNo(), a.getBusinessName(),
                a.getImprovementType() == null ? null : a.getImprovementType().getLabel(),
                a.getDefectType() == null ? null : a.getDefectType().getLabel(),
                a.getArtifactName(), a.getFileName(),
                a.getNonconformityLocation(), a.getNonconformityContent(),
                a.getActionPlan(), a.getAssignee(), a.getPlannedDate(),
                a.getStatus() == null ? null : a.getStatus().name(),
                a.getConfirmation(),
                a.getConfirmedDate() == null ? null : a.getConfirmedDate().toString());
    }
}
