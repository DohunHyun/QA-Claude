package com.nh.qagpt.domain.enums;

/** 검토 회차의 비동기 처리 상태. 최종 통과/불통과는 ReviewResult.passed 로 구분. */
public enum ReviewStatus {
    PENDING,     // 접수, 대기
    RUNNING,     // 4-Phase 검증 진행 중
    COMPLETED,   // 검증 완료 (passed 로 통과 여부 판단)
    FAILED       // 처리 실패 (파싱 오류 등)
}
