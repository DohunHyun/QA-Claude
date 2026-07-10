package com.nh.qagpt.dto;

import com.nh.qagpt.domain.Project;
import com.nh.qagpt.domain.ReportRequest;

public record ReportRequestDto(
        Long id,
        Long projectId,
        String projectCode,
        String projectName,
        String stageGroup,
        String status,
        String requestedBy,
        String requestedAt,
        String approvedBy,
        String approvedAt) {

    public static ReportRequestDto from(ReportRequest r) {
        Project p = r.getProject();
        return new ReportRequestDto(
                r.getId(),
                p == null ? null : p.getId(),
                p == null ? null : p.getCode(),
                p == null ? null : p.getName(),
                r.getStageGroup(),
                r.getStatus(),
                r.getRequestedBy(),
                r.getRequestedAt() == null ? null : r.getRequestedAt().toString(),
                r.getApprovedBy(),
                r.getApprovedAt() == null ? null : r.getApprovedAt().toString());
    }
}
