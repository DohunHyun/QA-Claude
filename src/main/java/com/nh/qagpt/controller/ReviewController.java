package com.nh.qagpt.controller;

import com.nh.qagpt.dto.ReviewResponse;
import com.nh.qagpt.exception.ResourceNotFoundException;
import com.nh.qagpt.repository.DefectRepository;
import com.nh.qagpt.repository.ReviewResultRepository;
import com.nh.qagpt.service.ReviewOrchestrator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewOrchestrator orchestrator;
    private final ReviewResultRepository reviewResultRepository;
    private final DefectRepository defectRepository;

    public ReviewController(ReviewOrchestrator orchestrator,
                            ReviewResultRepository reviewResultRepository,
                            DefectRepository defectRepository) {
        this.orchestrator = orchestrator;
        this.reviewResultRepository = reviewResultRepository;
        this.defectRepository = defectRepository;
    }

    /** 검토 트리거 — 백그라운드(reviewExecutor)에서 4-Phase 검증 실행. 즉시 202 응답. */
    @PostMapping
    public ResponseEntity<Map<String, Object>> trigger(@RequestParam Long documentId) {
        orchestrator.review(documentId);
        return ResponseEntity.accepted().body(Map.of(
                "message", "검증 접수됨",
                "documentId", documentId));
    }

    @GetMapping("/{id}")
    public ReviewResponse get(@PathVariable Long id) {
        return reviewResultRepository.findById(id)
                .map(r -> ReviewResponse.from(r, defectRepository.countByReviewResultId(r.getId())))
                .orElseThrow(() -> new ResourceNotFoundException("검토 결과 없음: " + id));
    }
}
