package com.nh.qagpt.dto;

import java.util.List;

/**
 * [S2] 업로드→검증 결과. passed=true & defectCount=0 이면 "결함 없음".
 * artifactType/artifactLabel: 자동 인식(또는 지정)된 산출물 유형.
 */
public record ValidationResultResponse(
        Long reviewId,
        String artifactType,
        String artifactLabel,
        boolean passed,
        int defectCount,
        String message,
        List<DefectDto> defects
) {}
