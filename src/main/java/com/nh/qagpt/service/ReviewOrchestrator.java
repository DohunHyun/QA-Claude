package com.nh.qagpt.service;

import com.nh.qagpt.domain.Defect;
import com.nh.qagpt.domain.Document;
import com.nh.qagpt.domain.ReviewResult;
import com.nh.qagpt.domain.enums.ArtifactType;
import com.nh.qagpt.domain.enums.ReviewStatus;
import com.nh.qagpt.domain.enums.Severity;
import com.nh.qagpt.repository.ReviewResultRepository;
import com.nh.qagpt.service.checklist.ChecklistEngine;
import com.nh.qagpt.service.classifier.DocumentClassifier;
import com.nh.qagpt.service.generator.ResultGenerator;
import com.nh.qagpt.service.parser.DocumentParserRouter;
import com.nh.qagpt.service.parser.ParsedDocument;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 검토 파이프라인 오케스트레이터.
 *
 * 4-Phase 고정 흐름 (spec §7.2 / PRD): parse → classify → checklist(4-Phase) → generate.
 *
 * [S1] {@link #runReview}: 동기 경로 — parse → checklist(최소검증) → 저장. 데모용으로 즉시 결과 반환.
 * TODO(S2+): classify(유형 자동인식) + generate(3종 결과물) + {@link #review} 비동기(파일 저장 후 id로 실행).
 */
@Service
public class ReviewOrchestrator {

    private final DocumentParserRouter parserRouter;
    private final DocumentClassifier classifier;
    private final ChecklistEngine checklistEngine;
    private final ResultGenerator resultGenerator;
    private final ReviewResultRepository reviewResultRepository;

    public ReviewOrchestrator(DocumentParserRouter parserRouter,
                              DocumentClassifier classifier,
                              ChecklistEngine checklistEngine,
                              ResultGenerator resultGenerator,
                              ReviewResultRepository reviewResultRepository) {
        this.parserRouter = parserRouter;
        this.classifier = classifier;
        this.checklistEngine = checklistEngine;
        this.resultGenerator = resultGenerator;
        this.reviewResultRepository = reviewResultRepository;
    }

    /**
     * [S1] 동기 검토: 업로드 즉시 파싱·검증하고 결과를 저장·반환한다.
     * 유형은 파라미터로 받는다(S2에서 classifier.classify()로 대체).
     */
    public ReviewResult runReview(Document document, byte[] content, ArtifactType type) {
        ReviewResult result = new ReviewResult();
        result.setDocument(document);
        result.setProject(document.getProject());
        result.setStage(type.getStage());
        result.setRound(1);

        try {
            ParsedDocument parsed = parserRouter.parse(content, document.getFileName(), document.getContentType());
            List<Defect> defects = checklistEngine.apply(parsed, type, document.getProject());
            defects.forEach(result::addDefect);

            boolean hasImprovement = defects.stream()
                    .anyMatch(d -> d.getSeverity() == Severity.IMPROVEMENT);
            result.setPassed(!hasImprovement);
            result.setStatus(ReviewStatus.COMPLETED);
        } catch (RuntimeException e) {
            result.setStatus(ReviewStatus.FAILED);
            throw e; // 파싱 실패 등은 상위(예외 핸들러)에서 사용자 메시지로 변환
        }

        return reviewResultRepository.save(result);
    }

    /** TODO(S2+): 비동기 전체 파이프라인 — 저장된 파일을 id로 로드해 classify+checklist+generate 실행. */
    @Async("reviewExecutor")
    public CompletableFuture<ReviewResult> review(Long documentId) {
        throw new UnsupportedOperationException("TODO(S2+): 비동기 4-Phase 오케스트레이션 (파일 저장 연동 필요)");
    }
}
