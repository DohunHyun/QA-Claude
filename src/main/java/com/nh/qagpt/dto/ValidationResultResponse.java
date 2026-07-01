package com.nh.qagpt.dto;

import java.util.List;

/** [S1] 업로드→검증 결과. passed=true & defectCount=0 이면 "결함 없음". */
public record ValidationResultResponse(
        Long reviewId,
        boolean passed,
        int defectCount,
        String message,
        List<DefectDto> defects
) {}
