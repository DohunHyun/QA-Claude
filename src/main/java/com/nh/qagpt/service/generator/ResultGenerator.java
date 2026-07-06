package com.nh.qagpt.service.generator;

import com.nh.qagpt.domain.ReviewResult;

/**
 * 결과물 품질의 핵심 시임. 3종 결과물을 생성한다.
 * 원본 포맷을 유지하고(HWPX→HWPX, Excel→Excel), 표시용 셀은 텍스트 서식으로 강제해 POI 부동소수점 오차를 막는다.
 */
public interface ResultGenerator {

    /**
     * ① AI 개선 산출물 — 개선(ERROR) 항목 위치에 {@code [개선]} 태그를 달아 변경 지점을 명시한다.
     * 원본 포맷(바이트)을 그대로 열어 구조를 유지하며, 빈 항목은 임의로 채우지 않는다(spec §4.6).
     * @param originalContent 검증했던 원본 파일 바이트 (포맷 유지를 위해 정규화 표현이 아닌 원본 사용)
     * @param fileName        포맷 판별용 원본 파일명
     */
    byte[] generateImprovedArtifact(ReviewResult result, byte[] originalContent, String fileName);

    /** ② 시정조치관리대장 — Excel, 표지·개정이력·시정조치서 3시트(본문 17열). */
    byte[] generateCorrectiveActionLedger(ReviewResult result);

    /** ③ 단계말 검토결과서 — HWPX 공식 문서(항목별 결과·결함유형·관점·근거). */
    byte[] generateReviewReport(ReviewResult result);
}
