package com.nh.qagpt.dto;

import com.nh.qagpt.domain.Defect;

public record DefectDto(
        String severity,
        String defectType,
        String perspective,
        String location,
        String description,
        String improvementGuide
) {
    public static DefectDto from(Defect d) {
        return new DefectDto(
                d.getSeverity() == null ? null : d.getSeverity().getLabel(),
                d.getDefectType() == null ? null : d.getDefectType().getLabel(),
                d.getPerspective() == null ? null : d.getPerspective().getLabel(),
                formatLocation(d),
                d.getDescription(),
                d.getImprovementGuide());
    }

    private static String formatLocation(Defect d) {
        StringBuilder sb = new StringBuilder();
        append(sb, "시트", d.getLocationSheet());
        append(sb, "열", d.getLocationColumn());
        append(sb, "행", d.getLocationRow());
        append(sb, "ID", d.getLocationId());
        return sb.toString();
    }

    private static void append(StringBuilder sb, String key, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(", ");
        }
        sb.append(key).append(":").append(value);
    }
}
