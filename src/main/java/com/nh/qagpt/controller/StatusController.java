package com.nh.qagpt.controller;

import com.nh.qagpt.dto.ProjectReviewStatusResponse;
import com.nh.qagpt.dto.ReviewResponse;
import com.nh.qagpt.repository.DefectRepository;
import com.nh.qagpt.repository.ReviewResultRepository;
import com.nh.qagpt.service.ReviewStatusService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class StatusController {

    private final ReviewResultRepository reviewResultRepository;
    private final DefectRepository defectRepository;
    private final ReviewStatusService reviewStatusService;

    public StatusController(ReviewResultRepository reviewResultRepository,
                            DefectRepository defectRepository,
                            ReviewStatusService reviewStatusService) {
        this.reviewResultRepository = reviewResultRepository;
        this.defectRepository = defectRepository;
        this.reviewStatusService = reviewStatusService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }

    /** 프로젝트별 회차 검증 이력(단순 목록). */
    @GetMapping("/projects/{projectId}/status")
    public List<ReviewResponse> projectStatus(@PathVariable Long projectId) {
        return reviewResultRepository.findByProjectIdOrderByRoundAsc(projectId).stream()
                .map(r -> ReviewResponse.from(r, defectRepository.countByReviewResultId(r.getId())))
                .toList();
    }

    /** [S6] 회차별 현황 — 산출물 유형별 회차 이력(대상/완료/잔여·통과회차) + 단계 게이트. */
    @GetMapping("/projects/{projectId}/review-status")
    public ProjectReviewStatusResponse reviewStatus(@PathVariable Long projectId) {
        return reviewStatusService.status(projectId);
    }
}
