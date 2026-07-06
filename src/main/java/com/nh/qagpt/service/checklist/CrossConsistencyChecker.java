package com.nh.qagpt.service.checklist;

import com.nh.qagpt.domain.Defect;
import com.nh.qagpt.domain.enums.DefectType;
import com.nh.qagpt.domain.enums.Perspective;
import com.nh.qagpt.domain.enums.Severity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * [S8] 교차 산출물 정합성 + 요구사항 양방향 매핑 (spec §7.2 Phase3, checklist_artifact_set_consistency).
 *
 * 순수 유틸리티(집합/건수 입력)로 단위검증 가능하게 설계했다. 프로젝트 단위 다중 산출물 파이프라인
 * 연동(파싱본 보관)은 후속 — 본 체커는 정합성 판정 로직을 담당한다.
 */
public class CrossConsistencyChecker {

    /** 목록형↔개별 건수 불일치. 두 산출물의 항목 수가 다르면 개선. */
    public List<Defect> rowCountMismatch(String labelA, int countA, String labelB, int countB) {
        List<Defect> defects = new ArrayList<>();
        if (countA != countB) {
            Defect d = base(Severity.IMPROVEMENT, DefectType.CONTENT_ERROR);
            d.setChecklistItemKey("artifact_set_consistency.row_count");
            d.setDescription("교차 산출물 건수 불일치: " + labelA + " " + countA + "건 vs " + labelB + " " + countB + "건");
            d.setImprovementGuide("두 산출물의 대상 건수를 일치시키거나 차이 사유를 명시하세요.");
            defects.add(d);
        }
        return defects;
    }

    /**
     * 요구사항추적표 양방향 매핑 누락.
     * forward=요구사항ID 집합, backward=대응(설계/구현)ID 집합.
     * - forward에만 있는 ID → 대응 누락(요구사항→설계 추적 끊김).
     * - backward에만 있는 ID → 근거 요구사항 누락(설계→요구사항 역추적 끊김).
     */
    public List<Defect> bidirectionalMapping(Set<String> forward, Set<String> backward,
                                             String forwardLabel, String backwardLabel) {
        List<Defect> defects = new ArrayList<>();
        for (String id : forward) {
            if (!backward.contains(id)) {
                defects.add(mappingDefect(id,
                        forwardLabel + " '" + id + "'에 대응하는 " + backwardLabel + " 매핑이 없습니다.",
                        backwardLabel + " 매핑을 추가하세요."));
            }
        }
        for (String id : backward) {
            if (!forward.contains(id)) {
                defects.add(mappingDefect(id,
                        backwardLabel + " '" + id + "'의 근거 " + forwardLabel + "이(가) 없습니다.",
                        "근거 " + forwardLabel + "을(를) 추가하거나 항목을 정리하세요."));
            }
        }
        return defects;
    }

    private Defect mappingDefect(String id, String desc, String guide) {
        Defect d = base(Severity.IMPROVEMENT, DefectType.MISSING_REQUIRED);
        d.setPerspective(Perspective.PROCESS); // 산출물 간 추적성 → 프로세스 관점
        d.setChecklistItemKey("requirement_traceability.bidirectional");
        d.setLocationId(id);
        d.setDescription(desc);
        d.setImprovementGuide(guide);
        return d;
    }

    private Defect base(Severity sev, DefectType type) {
        Defect d = new Defect();
        d.setSeverity(sev);
        d.setDefectType(type);
        d.setPerspective(Perspective.PROCESS);
        return d;
    }
}
