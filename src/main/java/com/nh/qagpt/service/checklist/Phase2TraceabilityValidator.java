package com.nh.qagpt.service.checklist;

import com.nh.qagpt.domain.Defect;
import com.nh.qagpt.domain.enums.ArtifactType;
import com.nh.qagpt.domain.enums.DefectType;
import com.nh.qagpt.domain.enums.Perspective;
import com.nh.qagpt.domain.enums.Severity;
import com.nh.qagpt.service.parser.ParsedDocument;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * [S8/후속] Phase2 요구사항 품질 보증 — 요구사항추적표(PM-310) 양방향 매핑 검증 (spec §7.2 Phase2).
 *
 * 실측 PM-310 본문(AP&B2B 시트) 컬럼: 요구사항 ID · 요구사항 명 · … · 액티비티 ID · U ID.
 * 검증: 요구사항 ID가 있는 행에 대응(설계) 매핑(액티비티 ID 또는 U ID)이 없으면 추적성 끊김(개선).
 * (역방향 설계→요구사항 매핑은 셀 내 다중값 구조라 교차 산출물 단계에서 다룬다 — CrossConsistencyChecker.)
 */
public class Phase2TraceabilityValidator {

    private static final String REQ_ID = "요구사항 ID";
    private static final String[] DESIGN_COLS = {"액티비티 ID", "U ID", "UI ID"};

    public List<Defect> validate(ParsedDocument document, ArtifactType type) {
        List<Defect> defects = new ArrayList<>();
        if (type != ArtifactType.REQUIREMENT_TRACEABILITY_MATRIX) {
            return defects;
        }
        Map.Entry<String, List<List<String>>> body = selectBodySheet(document);
        if (body == null) {
            return defects;
        }
        String sheet = body.getKey();
        List<List<String>> rows = body.getValue();

        int headerIdx = findHeaderRowIndex(rows);
        if (headerIdx < 0) {
            return defects;
        }
        List<String> header = rows.get(headerIdx);
        int reqCol = columnIndex(header, REQ_ID);
        if (reqCol < 0) {
            return defects; // 요구사항 ID 컬럼을 못 찾으면 검증 보류(오탐 방지)
        }
        List<Integer> designCols = new ArrayList<>();
        for (String dc : DESIGN_COLS) {
            int idx = columnIndex(header, dc);
            if (idx >= 0) {
                designCols.add(idx);
            }
        }

        for (int r = headerIdx + 1; r < rows.size(); r++) {
            List<String> row = rows.get(r);
            String reqId = cell(row, reqCol);
            if (reqId.isBlank()) {
                continue;
            }
            boolean hasDesign = designCols.stream().anyMatch(c -> !cell(row, c).isBlank());
            if (!designCols.isEmpty() && !hasDesign) {
                Defect d = new Defect();
                d.setSeverity(Severity.IMPROVEMENT);
                d.setDefectType(DefectType.MISSING_REQUIRED);
                d.setPerspective(Perspective.PROCESS); // 추적성 → 프로세스 관점
                d.setChecklistItemKey("requirement_traceability.forward_mapping");
                d.setLocationSheet(sheet);
                d.setLocationRow(String.valueOf(r));
                d.setLocationId(reqId);
                d.setDescription("요구사항 '" + reqId + "'에 대응하는 설계(액티비티/UI) 매핑이 없습니다.");
                d.setImprovementGuide("해당 요구사항의 설계 산출물 매핑(액티비티 ID 또는 U ID)을 기재하세요.");
                defects.add(d);
            }
        }
        return defects;
    }

    // ── 헬퍼 (Phase3ListValidator와 동일 규칙) ──────────────────────
    private Map.Entry<String, List<List<String>>> selectBodySheet(ParsedDocument document) {
        Map.Entry<String, List<List<String>>> byName = null;
        Map.Entry<String, List<List<String>>> byWidth = null;
        int maxWidth = -1;
        for (Map.Entry<String, List<List<String>>> e : document.getSheets().entrySet()) {
            String name = e.getKey() == null ? "" : e.getKey();
            // 요구사항 ID 컬럼을 가진 시트를 우선 선택(추적표 본문).
            if (headerHasReqId(e.getValue())) {
                return e;
            }
            int width = e.getValue().stream().mapToInt(List::size).max().orElse(0);
            if (width > maxWidth) {
                maxWidth = width;
                byWidth = e;
            }
            if (name.contains("본문") || name.contains("양식")) {
                byName = e;
            }
        }
        return byName != null ? byName : byWidth;
    }

    private boolean headerHasReqId(List<List<String>> rows) {
        int limit = Math.min(rows.size(), 12);
        for (int i = 0; i < limit; i++) {
            if (columnIndex(rows.get(i), REQ_ID) >= 0) {
                return true;
            }
        }
        return false;
    }

    private int findHeaderRowIndex(List<List<String>> rows) {
        int limit = Math.min(rows.size(), 12);
        for (int i = 0; i < limit; i++) {
            if (columnIndex(rows.get(i), REQ_ID) >= 0) {
                return i;
            }
        }
        return -1;
    }

    private int columnIndex(List<String> header, String token) {
        String target = normalize(token);
        for (int c = 0; c < header.size(); c++) {
            String h = normalize(header.get(c));
            if (h.equals(target) || h.contains(target)) {
                return c;
            }
        }
        return -1;
    }

    private String cell(List<String> row, int idx) {
        return idx >= 0 && idx < row.size() && row.get(idx) != null ? row.get(idx).trim() : "";
    }

    private String normalize(String s) {
        return s == null ? "" : s.replaceAll("\\s+", "").toLowerCase();
    }
}
