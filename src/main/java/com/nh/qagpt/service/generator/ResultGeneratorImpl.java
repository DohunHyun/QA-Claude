package com.nh.qagpt.service.generator;

import com.nh.qagpt.domain.ReviewResult;
import com.nh.qagpt.service.parser.ParsedDocument;
import org.springframework.stereotype.Service;

@Service
public class ResultGeneratorImpl implements ResultGenerator {

    @Override
    public byte[] generateImprovedArtifact(ReviewResult result, ParsedDocument original) {
        // TODO: 원본 구조 유지한 채 개선(ERROR) 항목 수정 + [개선] 태그 삽입 (Apache POI / HWPX 파서).
        throw new UnsupportedOperationException("TODO: AI 개선 산출물 생성");
    }

    @Override
    public byte[] generateCorrectiveActionLedger(ReviewResult result) {
        // TODO: 표지·개정이력·시정조치서 3시트, 본문 17열, No=CA_P##/CA_W##, 검토자=AI품질검토봇.
        //       버전·식별번호 셀은 텍스트 서식으로 강제(setCellType STRING).
        throw new UnsupportedOperationException("TODO: 시정조치관리대장 생성");
    }

    @Override
    public byte[] generateReviewReport(ReviewResult result) {
        // TODO: 단계말 검토결과서(HWPX) — 항목별 결과(개선/권고/OK)·결함유형·관점·근거 위치.
        throw new UnsupportedOperationException("TODO: 단계말 검토결과서 생성");
    }
}
