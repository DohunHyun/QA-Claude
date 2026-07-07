package com.nh.qagpt.service.checklist;

import com.nh.qagpt.domain.enums.ArtifactType;
import com.nh.qagpt.service.parser.ParsedDocument;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** [S8/후속] 교차 정합성용 요약 추출 — 본문 행수 + ID 목록. */
class ArtifactSummaryExtractorTest {

    private final ArtifactSummaryExtractor extractor = new ArtifactSummaryExtractor();

    @Test
    void 배치Job목록_행수와_JobID_추출() {
        ParsedDocument d = new ParsedDocument();
        d.getSheets().put("양식", List.of(
                List.of("단위업무명", "Batch Job ID", "업무명"),
                List.of("여신", "BJ-DE-0001", "a"),
                List.of("여신", "BJ-DE-0002", "b"),
                List.of("", "", "")));           // 빈 행은 제외
        ArtifactSummary s = extractor.extract(d, ArtifactType.BATCH_JOB_LIST);
        assertThat(s.bodyRowCount()).isEqualTo(2);
        assertThat(s.ids()).containsExactly("BJ-DE-0001", "BJ-DE-0002");
    }

    @Test
    void ID컬럼_없는유형은_행수만() {
        ParsedDocument d = new ParsedDocument();
        d.getSheets().put("양식", List.of(
                List.of("항목", "값"),
                List.of("a", "1")));
        ArtifactSummary s = extractor.extract(d, ArtifactType.TAILORING_RESULT);
        assertThat(s.bodyRowCount()).isEqualTo(1);
        assertThat(s.ids()).isEmpty();
    }
}
