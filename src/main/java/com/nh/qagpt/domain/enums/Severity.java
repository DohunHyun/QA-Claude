package com.nh.qagpt.domain.enums;

/**
 * 판정 심각도 (spec §3.1).
 * 개선(=ERROR): 반드시 시정 필요, 통과 불가(QA 예외 승인 시에만 예외).
 * 권고(=WARNING): 보완 권고, 통과 가능.
 */
public enum Severity {
    IMPROVEMENT("개선", "ERROR", false),
    RECOMMENDATION("권고", "WARNING", true);

    private final String label;   // UI 표기 (개선/권고)
    private final String alias;   // 배지 별칭 (ERROR/WARNING)
    private final boolean passable; // 이 항목만 있을 때 통과 가능 여부

    Severity(String label, String alias, boolean passable) {
        this.label = label;
        this.alias = alias;
        this.passable = passable;
    }

    public String getLabel() { return label; }
    public String getAlias() { return alias; }
    public boolean isPassable() { return passable; }
}
