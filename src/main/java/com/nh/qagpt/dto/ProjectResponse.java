package com.nh.qagpt.dto;

import com.nh.qagpt.domain.Project;

public record ProjectResponse(Long id, String name, String code, String status) {

    public static ProjectResponse from(Project p) {
        return new ProjectResponse(
                p.getId(), p.getName(), p.getCode(),
                p.getStatus() == null ? null : p.getStatus().name());
    }
}
