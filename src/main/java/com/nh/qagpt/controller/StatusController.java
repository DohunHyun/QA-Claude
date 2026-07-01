package com.nh.qagpt.controller;

import com.nh.qagpt.dto.ReviewResponse;
import com.nh.qagpt.repository.ReviewResultRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class StatusController {

    private final ReviewResultRepository reviewResultRepository;

    public StatusController(ReviewResultRepository reviewResultRepository) {
        this.reviewResultRepository = reviewResultRepository;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }

    /** 프로젝트별 회차 검증 이력(현황). */
    @GetMapping("/projects/{projectId}/status")
    public List<ReviewResponse> projectStatus(@PathVariable Long projectId) {
        return reviewResultRepository.findByProjectIdOrderByRoundAsc(projectId).stream()
                .map(ReviewResponse::from)
                .toList();
    }
}
