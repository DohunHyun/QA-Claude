package com.nh.qagpt.service.generator;

import com.nh.qagpt.domain.ReviewResult;
import com.nh.qagpt.service.parser.ParsedDocument;

/**
 * 결과물 품질의 핵심 시임. 3종 결과물을 생성한다.
 * 원본 포맷을 유지하고(HWPX→HWPX, Excel→Excel), 표시용 셀은 텍스트 서식으로 강제해 POI 부동소수점 오차를 막는다.
 */
public interface ResultGenerator {

    /** ① AI 개선 산출물 — 결함 수정 + [개선] 태그, 원본 포맷 유지. */
    byte[] generateImprovedArtifact(ReviewResult result, ParsedDocument original);

    /** ② 시정조치관리대장 — Excel, 표지·개정이력·시정조치서 3시트(본문 17열). */
    byte[] generateCorrectiveActionLedger(ReviewResult result);

    /** ③ 단계말 검토결과서 — HWPX 공식 문서(항목별 결과·결함유형·관점·근거). */
    byte[] generateReviewReport(ReviewResult result);
}
