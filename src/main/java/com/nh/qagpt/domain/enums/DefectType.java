package com.nh.qagpt.domain.enums;

/**
 * 결함유형 6종 (spec §3.2). 모든 판정에 함께 태깅한다.
 * 명명규칙 위반은 표준미준수의 하위 케이스로 판정한다.
 */
public enum DefectType {
    STANDARD_VIOLATION("표준미준수"),
    MISSING_REQUIRED("필수항목누락"),
    CONTENT_ERROR("내용오류·불명확"),
    NOT_SUBMITTED("미제출"),
    DUPLICATE("중복"),
    ETC("기타");

    private final String label;

    DefectType(String label) { this.label = label; }

    public String getLabel() { return label; }
}
