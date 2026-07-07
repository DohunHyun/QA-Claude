package com.nh.qagpt.service;

import com.nh.qagpt.domain.Defect;
import com.nh.qagpt.domain.Document;
import com.nh.qagpt.domain.ReviewResult;
import com.nh.qagpt.domain.enums.ArtifactType;
import com.nh.qagpt.domain.enums.ReviewStatus;
import com.nh.qagpt.domain.enums.Severity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nh.qagpt.repository.ReviewResultRepository;
import com.nh.qagpt.service.checklist.ArtifactSummary;
import com.nh.qagpt.service.checklist.ArtifactSummaryExtractor;
import com.nh.qagpt.service.checklist.ChecklistEngine;
import com.nh.qagpt.service.checklist.LlmChecklistEvaluator;
import com.nh.qagpt.service.classifier.DocumentClassifier;
import com.nh.qagpt.service.generator.ResultGenerator;
import com.nh.qagpt.service.parser.DocumentParserRouter;
import com.nh.qagpt.service.parser.ParsedDocument;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 검토 파이프라인 오케스트레이터.
 *
 * 4-Phase 고정 흐름 (spec §7.2 / PRD): parse → classify → checklist(4-Phase) → generate.
 *
 * [S1] {@link #runReview}: 동기 경로 — parse → (classify) → checklist(규칙) + LLM 판정 → 저장.
 * [S2] 유형 자동 인식 + LLM 판정 태깅 결합. API 키 미설정 시 규칙검증만 동작(우아한 저하).
 * TODO(S3+): generate(3종 결과물) + {@link #review} 비동기(파일 저장 후 id로 실행).
 */
@Service
public class ReviewOrchestrator {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final DocumentParserRouter parserRouter;
    private final DocumentClassifier classifier;
    private final ChecklistEngine checklistEngine;
    private final LlmChecklistEvaluator llmEvaluator;
    private final ResultGenerator resultGenerator;
    private final ReviewResultRepository reviewResultRepository;
    /** [S8/후속] 교차 정합성용 요약 추출 (상태 없음). */
    private final ArtifactSummaryExtractor summaryExtractor = new ArtifactSummaryExtractor();

    public ReviewOrchestrator(DocumentParserRouter parserRouter,
                              DocumentClassifier classifier,
                              ChecklistEngine checklistEngine,
                              LlmChecklistEvaluator llmEvaluator,
                              ResultGenerator resultGenerator,
                              ReviewResultRepository reviewResultRepository) {
        this.parserRouter = parserRouter;
        this.classifier = classifier;
        this.checklistEngine = checklistEngine;
        this.llmEvaluator = llmEvaluator;
        this.resultGenerator = resultGenerator;
        this.reviewResultRepository = reviewResultRepository;
    }

    /**
     * [S2] 동기 검토: 파싱 → 유형 확정(요청값 없으면 자동 인식) → 규칙검증 + LLM 판정 병합 → 저장.
     * @param requestedType 사용자가 지정한 유형(널/UNKNOWN이면 자동 인식)
     */
    public ReviewResult runReview(Document document, byte[] content, ArtifactType requestedType) {
        ReviewResult result = new ReviewResult();
        result.setDocument(document);
        result.setProject(document.getProject());

        try {
            ParsedDocument parsed = parserRouter.parse(content, document.getFileName(), document.getContentType());

            ArtifactType type = (requestedType == null || requestedType == ArtifactType.UNKNOWN)
                    ? classifier.classify(parsed)
                    : requestedType;
            document.setArtifactType(type);
            document.setStage(type.getStage());
            result.setStage(type.getStage());
            result.setRound(nextRound(document.getProject(), type)); // [S6] 재검증 시 회차 증가

            List<Defect> defects = new ArrayList<>(checklistEngine.apply(parsed, type, document.getProject()));
            defects.addAll(llmEvaluator.evaluate(parsed, type));
            defects.forEach(result::addDefect);

            boolean hasImprovement = defects.stream()
                    .anyMatch(d -> d.getSeverity() == Severity.IMPROVEMENT);
            result.setPassed(!hasImprovement);
            result.setStatus(ReviewStatus.COMPLETED);

            // [S8/후속] 교차 산출물 정합성용 요약 저장 (실패해도 검증은 계속).
            try {
                ArtifactSummary summary = summaryExtractor.extract(parsed, type);
                result.setRawResultJson(OBJECT_MAPPER.writeValueAsString(summary));
            } catch (Exception ignore) {
                // 요약 직렬화 실패는 검증 결과에 영향 없음
            }
        } catch (RuntimeException e) {
            result.setStatus(ReviewStatus.FAILED);
            throw e; // 파싱 실패 등은 상위(예외 핸들러)에서 사용자 메시지로 변환
        }

        return reviewResultRepository.save(result);
    }

    /** [S6] 같은 프로젝트·유형의 이전 회차 다음 번호. 프로젝트 미지정이면 1회차. */
    private int nextRound(com.nh.qagpt.domain.Project project, ArtifactType type) {
        if (project == null || project.getId() == null || type == null) {
            return 1;
        }
        return reviewResultRepository.maxRoundForArtifact(project.getId(), type) + 1;
    }

    /** TODO(S2+): 비동기 전체 파이프라인 — 저장된 파일을 id로 로드해 classify+checklist+generate 실행. */
    @Async("reviewExecutor")
    public CompletableFuture<ReviewResult> review(Long documentId) {
        throw new UnsupportedOperationException("TODO(S2+): 비동기 4-Phase 오케스트레이션 (파일 저장 연동 필요)");
    }
}
