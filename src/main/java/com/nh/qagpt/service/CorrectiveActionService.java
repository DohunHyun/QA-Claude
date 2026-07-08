package com.nh.qagpt.service;

import com.nh.qagpt.domain.CorrectiveAction;
import com.nh.qagpt.domain.Defect;
import com.nh.qagpt.domain.Document;
import com.nh.qagpt.domain.ReviewResult;
import com.nh.qagpt.domain.enums.ActionStatus;
import com.nh.qagpt.domain.enums.ArtifactType;
import com.nh.qagpt.domain.enums.Perspective;
import com.nh.qagpt.exception.ResourceNotFoundException;
import com.nh.qagpt.repository.CorrectiveActionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 시정조치 라인 영속화·상태 추적 (spec §4.4·§8.2).
 * 검증 완료 시 결함마다 CorrectiveAction 라인을 생성(No=CA_P##/CA_W##, 검토자=AI품질검토봇)하고,
 * PM/QA가 조치 상태(대상→진행→완료)·담당자·일정을 갱신한다. 시정조치관리대장 Excel은 이 라인을 기준으로 생성된다.
 */
@Service
public class CorrectiveActionService {

    private final CorrectiveActionRepository correctiveActionRepository;

    public CorrectiveActionService(CorrectiveActionRepository correctiveActionRepository) {
        this.correctiveActionRepository = correctiveActionRepository;
    }

    /** 검증 결과의 결함들로 시정조치 라인 생성·저장 (매 검증마다 — spec §4.4). */
    @Transactional
    public List<CorrectiveAction> createFromReview(ReviewResult result) {
        return correctiveActionRepository.saveAll(buildFromReview(result));
    }

    /** 결함 → 시정조치 라인 매핑(저장 없음). 대장 생성기의 폴백(미영속 결과)에도 사용된다. */
    public List<CorrectiveAction> buildFromReview(ReviewResult result) {
        List<CorrectiveAction> actions = new ArrayList<>();
        Document doc = result.getDocument();
        String artifactName = artifactLabel(doc);
        String fileName = doc == null || doc.getFileName() == null ? "" : doc.getFileName();
        LocalDate reviewDate = result.getCreatedAt() == null
                ? LocalDate.now()
                : result.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate();

        int pSeq = 0, wSeq = 0;
        for (Defect d : result.getDefects()) {
            boolean isProcess = d.getPerspective() == Perspective.PROCESS;
            CorrectiveAction action = new CorrectiveAction();
            action.setReviewResult(result);
            action.setNo(isProcess ? String.format("CA_P%02d", ++pSeq) : String.format("CA_W%02d", ++wSeq));
            action.setBusinessName(isProcess ? "프로젝트관리" : "개발산출물");
            action.setImprovementType(d.getSeverity());
            action.setReviewDate(reviewDate);
            action.setArtifactName(artifactName);
            action.setFileName(fileName);
            action.setNonconformityLocation(location(d));
            action.setDefectType(d.getDefectType());
            action.setNonconformityContent(d.getDescription());
            action.setActionPlan(d.getImprovementGuide());
            action.setStatus(ActionStatus.TARGET);
            actions.add(action);
        }
        return actions;
    }

    /** 조치 상태·계획 갱신 (PM 수정 현황 추적). DONE 전이 시 조치완료일 자동 기록. */
    @Transactional
    public CorrectiveAction update(Long actionId, ActionStatus status, String assignee,
                                   String plannedDate, String confirmation) {
        CorrectiveAction action = correctiveActionRepository.findById(actionId)
                .orElseThrow(() -> new ResourceNotFoundException("시정조치 항목 없음: " + actionId));
        if (status != null) {
            action.setStatus(status);
            if (status == ActionStatus.DONE && action.getConfirmedDate() == null) {
                action.setConfirmedDate(LocalDate.now());
            }
        }
        if (assignee != null && !assignee.isBlank()) {
            action.setAssignee(assignee);
        }
        if (plannedDate != null && !plannedDate.isBlank()) {
            action.setPlannedDate(plannedDate);
        }
        if (confirmation != null && !confirmation.isBlank()) {
            action.setConfirmation(confirmation);
        }
        return correctiveActionRepository.save(action);
    }

    @Transactional(readOnly = true)
    public List<CorrectiveAction> byReview(Long reviewResultId) {
        return correctiveActionRepository.findByReviewResultId(reviewResultId);
    }

    private String artifactLabel(Document doc) {
        if (doc == null || doc.getArtifactType() == null || doc.getArtifactType() == ArtifactType.UNKNOWN) {
            return "";
        }
        return doc.getArtifactType().getLabel();
    }

    private String location(Defect d) {
        StringBuilder sb = new StringBuilder();
        append(sb, "시트", d.getLocationSheet());
        append(sb, "행", d.getLocationRow());
        append(sb, "열", d.getLocationColumn());
        append(sb, "ID", d.getLocationId());
        return sb.toString();
    }

    private void append(StringBuilder sb, String key, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(", ");
        }
        sb.append(key).append(":").append(value);
    }
}
