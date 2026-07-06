package com.nh.qagpt.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/** 공통 예외 처리. 미구현 시임(UnsupportedOperationException)은 501로 응답해 골격 상태를 드러낸다. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException e) {
        return body(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .orElse("잘못된 요청");
        return body(HttpStatus.BAD_REQUEST, msg);
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<Map<String, Object>> handleNotImplemented(UnsupportedOperationException e) {
        return body(HttpStatus.NOT_IMPLEMENTED, e.getMessage());
    }

    /** 파싱 실패 등 잘못된 입력 (S1: 업로드 파일 파싱 실패 시 사용자 메시지). */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadInput(IllegalArgumentException e) {
        return body(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException e) {
        return body(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    /** [S7] QA 승인 절차 위반 — 승인 전 발급/예외 승인 누락. */
    @ExceptionHandler(QaApprovalException.class)
    public ResponseEntity<Map<String, Object>> handleApproval(QaApprovalException e) {
        return body(HttpStatus.CONFLICT, e.getMessage());
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message == null ? "" : message));
    }
}
