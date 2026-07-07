package com.nh.qagpt.exception;

/**
 * [S7] QA 승인 절차 위반 (승인 전 발급 시도, 개선 잔존인데 예외 승인 없이 승인 시도 등).
 * 409 CONFLICT로 응답한다.
 */
public class QaApprovalException extends RuntimeException {
    public QaApprovalException(String message) {
        super(message);
    }
}
