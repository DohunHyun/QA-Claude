package com.nh.qagpt.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nh.qagpt.domain.Defect;
import com.nh.qagpt.domain.Document;
import com.nh.qagpt.domain.Project;
import com.nh.qagpt.domain.ReviewResult;
import com.nh.qagpt.domain.enums.ArtifactType;
import com.nh.qagpt.domain.enums.ReviewStatus;
import com.nh.qagpt.domain.enums.Severity;
import com.nh.qagpt.exception.ResourceNotFoundException;
import com.nh.qagpt.repository.DocumentRepository;
import com.nh.qagpt.repository.ReviewResultRepository;
import com.nh.qagpt.service.checklist.ArtifactSummary;
import com.nh.qagpt.service.checklist.ArtifactSummaryExtractor;
import com.nh.qagpt.service.checklist.ChecklistEngine;
import com.nh.qagpt.service.checklist.LlmChecklistEvaluator;
import com.nh.qagpt.service.classifier.DocumentClassifier;
import com.nh.qagpt.service.parser.DocumentParserRouter;
import com.nh.qagpt.service.parser.ParsedDocument;
import com.nh.qagpt.service.storage.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 검토 파이프라인 오케스트레이터.
 *
 * 4-Phase 고정 흐름 (spec §7.2 / PRD): parse → classify → checklist(4-Phase) → 결과 저장.
 *
 * - {@link #runReview} 동기 경로: 업로드 즉시 검증·응답 (데모 기본 흐름).
 * - {@link #review} 비동기 경로 (spec §10.1): 저장된 원본(storagePath)을 로드해 백그라운드 검증.
 *   진행 상태는 ReviewResult.status(PENDING→RUNNING→COMPLETED/FAILED)로 폴링한다.
 */
@Service
public class ReviewOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(ReviewOrchestrator.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final DocumentParserRouter parserRouter;
    private final DocumentClassifier classifier;
    private final ChecklistEngine checklistEngine;
    private final LlmChecklistEvaluator llmEvaluator;
    private final ReviewResultRepository reviewResultRepository;
    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorage;
    /** [S8/후속] 교차 정합성용 요약 추출 (상태 없음). */
    private final ArtifactSummaryExtractor summaryExtractor = new ArtifactSummaryExtractor();

    public ReviewOrchestrator(DocumentParserRouter parserRouter,
                              DocumentClassifier classifier,
                              ChecklistEngine checklistEngine,
                              LlmChecklistEvaluator llmEvaluator,
                              ReviewResultRepository reviewResultRepository,
                              DocumentRepository documentRepository,
                              FileStorageService fileStorage) {
        this.parserRouter = parserRouter;
        this.classifier = classifier;
        this.checklistEngine = checklistEngine;
        this.llmEvaluator = llmEvaluator;
        this.reviewResultRepository = reviewResultRepository;
        this.documentRepository = documentRepository;
        this.fileStorage = fileStorage;
    }

    /**
     * 동기 검토: 파싱 → 유형 확정(요청값 없으면 자동 인식) → 규칙검증 + LLM 판정 병합 → 저장.
     * @param requestedType 사용자가 지정한 유형(널/UNKNOWN이면 자동 인식)
     */
    public ReviewResult runReview(Document document, byte[] content, ArtifactType requestedType) {
        ReviewResult result = new ReviewResult();
        result.setDocument(document);
        result.setProject(document.getProject());

        try {
            execute(result, document, content, requestedType);
        } catch (RuntimeException e) {
            result.setStatus(ReviewStatus.FAILED);
            throw e; // 파싱 실패 등은 상위(예외 핸들러)에서 사용자 메시지로 변환
        }
        return reviewResultRepository.save(result);
    }

    /**
     * 비동기 검토 (spec §10.1): 저장된 원본을 로드해 백그라운드에서 4-Phase 검증.
     * RUNNING 상태의 ReviewResult를 즉시 저장해 진행도 폴링이 가능하다. 실패는 FAILED로 기록(예외 전파 없음).
     */
    @Async("reviewExecutor")
    @Transactional
    public CompletableFuture<ReviewResult> review(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("산출물 없음: " + documentId));

        ReviewResult result = new ReviewResult();
        result.setDocument(document);
        result.setProject(document.getProject());
        result.setStatus(ReviewStatus.RUNNING);
        result.setStage(document.getStage());
        reviewResultRepository.save(result); // 진행도 폴링용으로 즉시 노출

        try {
            byte[] content = fileStorage.load(document.getStoragePath());
            execute(result, document, content, document.getArtifactType());
            documentRepository.save(document); // 자동 인식된 유형/단계 영속화
        } catch (RuntimeException e) {
            log.warn("비동기 검증 실패(document={}): {}", documentId, e.getMessage());
            result.setStatus(ReviewStatus.FAILED);
        }
        return CompletableFuture.completedFuture(reviewResultRepository.save(result));
    }

    /** 검증 코어: 파싱→유형확정→회차→규칙+LLM 판정→통과여부→교차정합 요약. 예외는 호출측에서 처리. */
    private void execute(ReviewResult result, Document document, byte[] content, ArtifactType requestedType) {
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
    }

    /** [S6] 같은 프로젝트·유형의 이전 회차 다음 번호. 프로젝트 미지정이면 1회차. */
    private int nextRound(Project project, ArtifactType type) {
        if (project == null || project.getId() == null || type == null) {
            return 1;
        }
        return reviewResultRepository.maxRoundForArtifact(project.getId(), type) + 1;
    }
}
