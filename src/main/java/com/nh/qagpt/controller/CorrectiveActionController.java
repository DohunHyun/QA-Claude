package com.nh.qagpt.controller;

import com.nh.qagpt.domain.enums.ActionStatus;
import com.nh.qagpt.dto.CorrectiveActionDto;
import com.nh.qagpt.service.CorrectiveActionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * [spec §4.4·§8.2] 시정조치 상태 추적 — 검증마다 생성된 라인의 조치 상태(대상→진행→완료)·담당자·일정 관리.
 */
@RestController
@RequestMapping("/api")
public class CorrectiveActionController {

    private final CorrectiveActionService correctiveActionService;

    public CorrectiveActionController(CorrectiveActionService correctiveActionService) {
        this.correctiveActionService = correctiveActionService;
    }

    /** 검토 회차의 시정조치 라인 목록. */
    @GetMapping("/reviews/{reviewId}/corrective-actions")
    public List<CorrectiveActionDto> byReview(@PathVariable Long reviewId) {
        return correctiveActionService.byReview(reviewId).stream()
                .map(CorrectiveActionDto::from)
                .toList();
    }

    /** 조치 상태·담당자·일정 갱신. DONE 전이 시 조치완료일 자동 기록. */
    @PatchMapping("/corrective-actions/{id}")
    public CorrectiveActionDto update(@PathVariable Long id,
                                      @RequestParam(required = false) ActionStatus status,
                                      @RequestParam(required = false) String assignee,
                                      @RequestParam(required = false) String plannedDate,
                                      @RequestParam(required = false) String confirmation) {
        return CorrectiveActionDto.from(
                correctiveActionService.update(id, status, assignee, plannedDate, confirmation));
    }
}
