package com.nh.qagpt.domain.enums;

/**
 * 점검 관점 2종 (spec §2.4). 모든 검증은 두 관점으로 수행한다.
 * 산출물: 각 산출물 자체의 품질(완전성·일관성·정확성·표준준수·필수항목).
 * 프로세스: 산출물 간 관계 및 관리 프로세스(추적성·변경관리·승인·현행화).
 */
public enum Perspective {
    ARTIFACT("산출물"),
    PROCESS("프로세스");

    private final String label;

    Perspective(String label) { this.label = label; }

    public String getLabel() { return label; }
}
