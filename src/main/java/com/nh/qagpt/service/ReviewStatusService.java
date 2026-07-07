package com.nh.qagpt.service;

import com.nh.qagpt.domain.Defect;
import com.nh.qagpt.domain.Document;
import com.nh.qagpt.domain.ReviewResult;
import com.nh.qagpt.domain.enums.ArtifactType;
import com.nh.qagpt.domain.enums.Severity;
import com.nh.qagpt.domain.enums.Stage;
import com.nh.qagpt.dto.ProjectReviewStatusResponse;
import com.nh.qagpt.dto.ProjectReviewStatusResponse.ArtifactHistory;
import com.nh.qagpt.dto.ProjectReviewStatusResponse.RoundSummary;
import com.nh.qagpt.dto.ProjectReviewStatusResponse.StageGate;
import com.nh.qagpt.repository.ReviewResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * [S6] 회차별 검증 현황 집계. 산출물 유형별 회차 이력과 단계 게이트를 구성한다.
 * lazy 연관(document/defects) 접근을 위해 트랜잭션 내에서 조회한다.
 */
@Service
public class ReviewStatusService {

    /** 단계 게이팅 순서 (spec §7.1: 관리→분석→설계). */
    private static final Stage[] STAGE_ORDER = {Stage.MANAGEMENT, Stage.ANALYSIS, Stage.DESIGN};

    private final ReviewResultRepository reviewResultRepository;

    public ReviewStatusService(ReviewResultRepository reviewResultRepository) {
        this.reviewResultRepository = reviewResultRepository;
    }

    @Transactional(readOnly = true)
    public ProjectReviewStatusResponse status(Long projectId) {
        List<ReviewResult> results = reviewResultRepository.findByProjectIdOrderByRoundAsc(projectId);

        // 유형별 그룹 (등록 순서 보존)
        Map<ArtifactType, List<ReviewResult>> byType = new LinkedHashMap<>();
        for (ReviewResult r : results) {
            ArtifactType type = typeOf(r);
            byType.computeIfAbsent(type, k -> new ArrayList<>()).add(r);
        }

        List<ArtifactHistory> histories = new ArrayList<>();
        for (Map.Entry<ArtifactType, List<ReviewResult>> e : byType.entrySet()) {
            histories.add(toHistory(e.getKey(), e.getValue()));
        }

        List<StageGate> gates = toStageGates(histories);
        return new ProjectReviewStatusResponse(projectId, histories, gates);
    }

    private ArtifactHistory toHistory(ArtifactType type, List<ReviewResult> results) {
        List<RoundSummary> rounds = new ArrayList<>();
        Integer passedAtRound = null;
        for (ReviewResult r : results) {
            int improvement = 0, recommendation = 0;
            for (Defect d : r.getDefects()) {
                if (d.getSeverity() == Severity.IMPROVEMENT) {
                    improvement++;
                } else {
                    recommendation++;
                }
            }
            int target = improvement + recommendation;
            int completed = 0;            // 완료 추적(시정조치 확인)은 후속 — 현재 0
            int remaining = target - completed;
            rounds.add(new RoundSummary(r.getRound(), r.isPassed(),
                    target, completed, remaining, improvement, recommendation));
            if (passedAtRound == null && r.isPassed()) {
                passedAtRound = r.getRound();
            }
        }
        boolean latestPassed = !results.isEmpty() && results.get(results.size() - 1).isPassed();
        Stage stage = type.getStage();
        return new ArtifactHistory(
                type.name(), type.getLabel(),
                stage == null ? null : stage.getLabel(),
                latestPassed, passedAtRound, rounds);
    }

    private List<StageGate> toStageGates(List<ArtifactHistory> histories) {
        List<StageGate> gates = new ArrayList<>();
        for (int i = 0; i < STAGE_ORDER.length; i++) {
            Stage stage = STAGE_ORDER[i];
            String label = stage.getLabel();
            List<ArtifactHistory> inStage = histories.stream()
                    .filter(h -> label.equals(h.stage()))
                    .toList();
            // 해당 단계에 검증된 산출물이 있고, 모두 최신 회차 통과 시 단계 통과.
            boolean passed = !inStage.isEmpty() && inStage.stream().allMatch(ArtifactHistory::passed);
            String nextStage = (i + 1 < STAGE_ORDER.length) ? STAGE_ORDER[i + 1].getLabel() : null;
            boolean nextUnlocked = passed && nextStage != null;
            gates.add(new StageGate(stage.name(), label, passed, nextUnlocked, nextStage));
        }
        return gates;
    }

    private ArtifactType typeOf(ReviewResult r) {
        Document doc = r.getDocument();
        if (doc == null || doc.getArtifactType() == null) {
            return ArtifactType.UNKNOWN;
        }
        return doc.getArtifactType();
    }
}
