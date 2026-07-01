package com.nh.qagpt.domain.enums;

/** 프로젝트 상태. PM 신청 → 관리자 승인 → 활성(검증 시작 가능). */
public enum ProjectStatus {
    REQUESTED,   // PM 신청
    APPROVED,    // 관리자 승인
    ACTIVE       // 활성 (검증 진행)
}
