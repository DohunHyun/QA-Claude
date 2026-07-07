package com.nh.qagpt.service.checklist;

import com.nh.qagpt.config.ClaudeProperties;
import com.nh.qagpt.domain.ChecklistItem;
import com.nh.qagpt.domain.Defect;
import com.nh.qagpt.domain.enums.ArtifactType;
import com.nh.qagpt.domain.enums.DefectType;
import com.nh.qagpt.domain.enums.Perspective;
import com.nh.qagpt.domain.enums.Severity;
import com.nh.qagpt.repository.ChecklistItemRepository;
import com.nh.qagpt.service.ai.ClaudeClient;
import com.nh.qagpt.service.parser.ParsedDocument;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** [S2] LLM 판정 — 응답 JSON→Defect 태깅 매핑, 근거위치 유지, 우아한 저하 검증. */
class LlmChecklistEvaluatorTest {

    private final ChecklistItemRepository repo = mock(ChecklistItemRepository.class);

    private ClaudeProperties propsWithKey() {
        ClaudeProperties p = new ClaudeProperties();
        p.setApiKey("test-key");
        return p;
    }

    private ChecklistItem item(String key) {
        ChecklistItem it = new ChecklistItem();
        it.setArtifactType(ArtifactType.BATCH_JOB_LIST);
        it.setItemKey(key);
        it.setSeverity(Severity.IMPROVEMENT);
        it.setDescription("필수 컬럼 존재 여부");
        return it;
    }

    private ParsedDocument doc() {
        ParsedDocument d = new ParsedDocument();
        d.setFileName("NHEFS-EA-AN07-배치Job목록.xlsx");
        d.getSheets().put("본문", List.of(List.of("단위업무명", "배치Job ID")));
        return d;
    }

    @Test
    void 응답JSON을_결함으로_태깅한다() {
        when(repo.findByArtifactType(ArtifactType.BATCH_JOB_LIST)).thenReturn(List.of(item("batch_job_list-05")));
        ClaudeClient client = fixedResponse("""
                [
                  {
                    "severity": "개선",
                    "defectType": "필수항목누락",
                    "perspective": "산출물",
                    "locationSheet": "본문", "locationRow": "12", "locationColumn": "업무명", "locationId": "",
                    "description": "업무명이 비어 있음",
                    "improvementGuide": "업무명을 기재하세요",
                    "checklistItemKey": "batch_job_list-05"
                  }
                ]
                """);
        LlmChecklistEvaluator evaluator = new LlmChecklistEvaluatorImpl(client, propsWithKey(), repo);

        List<Defect> defects = evaluator.evaluate(doc(), ArtifactType.BATCH_JOB_LIST);

        assertThat(defects).hasSize(1);
        Defect d = defects.get(0);
        assertThat(d.getSeverity()).isEqualTo(Severity.IMPROVEMENT);
        assertThat(d.getDefectType()).isEqualTo(DefectType.MISSING_REQUIRED);
        assertThat(d.getPerspective()).isEqualTo(Perspective.ARTIFACT);
        assertThat(d.getLocationSheet()).isEqualTo("본문");
        assertThat(d.getLocationColumn()).isEqualTo("업무명");
        assertThat(d.getLocationId()).isNull(); // 빈 문자열은 null로 정규화
        assertThat(d.getChecklistItemKey()).isEqualTo("batch_job_list-05");
    }

    @Test
    void 코드펜스가_섞여도_배열만_추출한다() {
        when(repo.findByArtifactType(any())).thenReturn(List.of(item("k")));
        ClaudeClient client = fixedResponse("판정 결과입니다:\n```json\n[{\"severity\":\"권고\",\"description\":\"x\"}]\n```");
        LlmChecklistEvaluator evaluator = new LlmChecklistEvaluatorImpl(client, propsWithKey(), repo);

        List<Defect> defects = evaluator.evaluate(doc(), ArtifactType.BATCH_JOB_LIST);

        assertThat(defects).hasSize(1);
        assertThat(defects.get(0).getSeverity()).isEqualTo(Severity.RECOMMENDATION);
    }

    @Test
    void API키_없으면_LLM호출없이_빈목록() {
        AtomicInteger calls = new AtomicInteger();
        ClaudeClient counting = new ClaudeClient() {
            @Override public String complete(String s, String u) { calls.incrementAndGet(); return "[]"; }
            @Override public String complete(String s, String u, double t) { calls.incrementAndGet(); return "[]"; }
        };
        LlmChecklistEvaluator evaluator = new LlmChecklistEvaluatorImpl(counting, new ClaudeProperties(), repo);

        List<Defect> defects = evaluator.evaluate(doc(), ArtifactType.BATCH_JOB_LIST);

        assertThat(defects).isEmpty();
        assertThat(calls).hasValue(0);
    }

    @Test
    void UNKNOWN유형은_LLM호출없이_빈목록() {
        AtomicInteger calls = new AtomicInteger();
        ClaudeClient counting = new ClaudeClient() {
            @Override public String complete(String s, String u) { calls.incrementAndGet(); return "[]"; }
            @Override public String complete(String s, String u, double t) { calls.incrementAndGet(); return "[]"; }
        };
        LlmChecklistEvaluator evaluator = new LlmChecklistEvaluatorImpl(counting, propsWithKey(), repo);

        assertThat(evaluator.evaluate(doc(), ArtifactType.UNKNOWN)).isEmpty();
        assertThat(calls).hasValue(0);
    }

    @Test
    void LLM예외는_삼켜지고_빈목록으로_저하() {
        when(repo.findByArtifactType(any())).thenReturn(List.of(item("k")));
        ClaudeClient failing = new ClaudeClient() {
            @Override public String complete(String s, String u) { return complete(s, u, 0.0); }
            @Override public String complete(String s, String u, double t) { throw new RuntimeException("api down"); }
        };
        LlmChecklistEvaluator evaluator = new LlmChecklistEvaluatorImpl(failing, propsWithKey(), repo);

        assertThat(evaluator.evaluate(doc(), ArtifactType.BATCH_JOB_LIST)).isEmpty();
    }

    private ClaudeClient fixedResponse(String response) {
        ClaudeClient c = mock(ClaudeClient.class);
        when(c.complete(any(), any(), org.mockito.ArgumentMatchers.anyDouble())).thenReturn(response);
        return c;
    }
}
