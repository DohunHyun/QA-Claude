package com.nh.qagpt.service;

import com.nh.qagpt.domain.ReviewResult;
import com.nh.qagpt.repository.DocumentRepository;
import com.nh.qagpt.repository.ReviewResultRepository;
import com.nh.qagpt.service.checklist.ChecklistEngine;
import com.nh.qagpt.service.classifier.DocumentClassifier;
import com.nh.qagpt.service.generator.ResultGenerator;
import com.nh.qagpt.service.parser.DocumentParserRouter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 검토 파이프라인 오케스트레이터. reviewExecutor 위에서 비동기 실행되어 업로드 응답 지연을 막는다.
 *
 * 4-Phase 고정 흐름 (spec §7.2 / PRD):
 *   1) parse(파싱) → 2) classify(유형 인식) → 3) checklist(4-Phase 검증) → 4) generate(3종 결과물)
 * 순서를 건너뛰거나 역행하지 않는다.
 */
@Service
public class ReviewOrchestrator {

    private final DocumentParserRouter parserRouter;
    private final DocumentClassifier classifier;
    private final ChecklistEngine checklistEngine;
    private final ResultGenerator resultGenerator;
    private final DocumentRepository documentRepository;
    private final ReviewResultRepository reviewResultRepository;

    public ReviewOrchestrator(DocumentParserRouter parserRouter,
                              DocumentClassifier classifier,
                              ChecklistEngine checklistEngine,
                              ResultGenerator resultGenerator,
                              DocumentRepository documentRepository,
                              ReviewResultRepository reviewResultRepository) {
        this.parserRouter = parserRouter;
        this.classifier = classifier;
        this.checklistEngine = checklistEngine;
        this.resultGenerator = resultGenerator;
        this.documentRepository = documentRepository;
        this.reviewResultRepository = reviewResultRepository;
    }

    @Async("reviewExecutor")
    public CompletableFuture<ReviewResult> review(Long documentId) {
        // TODO: 아래 4-Phase를 구현한다.
        //   Document doc = documentRepository.findById(documentId)...;
        //   ParsedDocument parsed = parserRouter.parse(...);              // Phase 1
        //   ArtifactType type = classifier.classify(parsed);             // Phase 2 (유형 인식)
        //   List<Defect> defects = checklistEngine.apply(parsed, type, doc.getProject()); // Phase 3
        //   ReviewResult saved = reviewResultRepository.save(...);        // 개선 항목 유무로 passed 결정
        //   resultGenerator.generate...();                               // Phase 4
        throw new UnsupportedOperationException("TODO: 4-Phase 검증 오케스트레이션 구현");
    }
}
