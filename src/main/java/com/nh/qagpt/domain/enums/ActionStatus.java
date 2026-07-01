package com.nh.qagpt.domain.enums;

/** 시정조치 상태 (시정조치관리대장 라인의 진행 상태). */
public enum ActionStatus {
    TARGET,        // 대상 (조치 필요)
    IN_PROGRESS,   // 진행
    DONE           // 완료
}
