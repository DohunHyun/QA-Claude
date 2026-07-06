package com.nh.qagpt.dto;

import java.util.List;

/**
 * [S6] 프로젝트 회차별 검증 현황.
 * - artifacts: 산출물 유형별 회차 이력(대상/완료/잔여 집계, 통과 회차)
 * - stages: 단계 게이트(통과 시 다음 단계 진행 가능)
 */
public record ProjectReviewStatusResponse(
        Long projectId,
        List<ArtifactHistory> artifacts,
        List<StageGate> stages
) {

    /** 산출물 유형별 회차 이력. */
    public record ArtifactHistory(
            String artifactType,
            String label,
            String stage,
            boolean passed,          // 최신 회차 통과 여부
            Integer passedAtRound,   // 최초 통과 회차(미통과면 null)
            List<RoundSummary> rounds
    ) {}

    /** 한 회차의 집계. target=대상건수, completed=완료, remaining=잔여. */
    public record RoundSummary(
            int round,
            boolean passed,
            int target,
            int completed,
            int remaining,
            int improvementCount,   // 개선(ERROR)
            int recommendationCount // 권고(WARNING)
    ) {}

    /** 단계 게이트: 해당 단계 전 산출물 통과 시 다음 단계 진행 가능. */
    public record StageGate(
            String stage,
            String label,
            boolean passed,
            boolean nextStageUnlocked,
            String nextStage
    ) {}
}
