package com.nh.qagpt.domain.enums;

/**
 * 프로젝트 단계. MVP 검증 대상은 관리·분석·설계 3단계 (테스트·이행은 Phase 3+).
 * 단계 게이팅: 관리 통과 → 분석, 분석 통과 → 설계.
 */
public enum Stage {
    MANAGEMENT("관리"),
    ANALYSIS("분석"),
    DESIGN("설계");

    private final String label;

    Stage(String label) { this.label = label; }

    public String getLabel() { return label; }
}
