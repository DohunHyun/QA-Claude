package com.nh.qagpt.service.checklist;

import com.nh.qagpt.domain.Defect;
import com.nh.qagpt.domain.enums.ArtifactType;
import com.nh.qagpt.domain.enums.DefectType;
import com.nh.qagpt.domain.enums.Perspective;
import com.nh.qagpt.domain.enums.Severity;
import com.nh.qagpt.service.parser.ParsedDocument;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * [S8/P1-F] Phase3 목록/정합성 검증 (spec §7.2 Phase3) — 목록형 산출물 확대.
 *
 * 실측 컬럼(신 포맷 NHEFS + 구 템플릿 NHXXX) 기준:
 *  - 프로그램목록(DS07): 단위업무명·프로그램 ID·프로그램 명
 *  - 인터페이스정의서(AN08): 단위업무명·인터페이스ID·인터페이스명
 *  - UI목록(DS01, 구 템플릿 실측): 단위업무명·화면/보고서ID·화면/보고서명
 *  - 요구사항정의서(AN06): 요구사항 ID·요구사항명 (신 포맷 '기능' 시트는 멀티행 헤더 — r0 그룹 + r2 상세)
 *  - 배치Job목록(AN07): 필수컬럼은 {@link ChecklistEngineImpl}가 담당, 여기선 ID중복·enum(개발구분)만.
 *
 * 멀티행 헤더 대응: 필수컬럼은 헤더 존(상위 12행) 내 등장 여부로, ID열은 토큰이 있는 (행,열)을 찾아
 * 그 아래를 데이터로 본다. 공통: ID 중복(중복 결함), 필수컬럼 존재, 열거값 유효성.
 */
public class Phase3ListValidator {

    /** 유형별 목록형 필수컬럼 (배치Job목록은 엔진이 담당하므로 제외). */
    private static final Map<ArtifactType, List<String>> REQUIRED = new LinkedHashMap<>();
    /** 유형별 ID 컬럼(중복 검사 대상). */
    private static final Map<ArtifactType, String> ID_COLUMN = new LinkedHashMap<>();
    /** 배치Job목록 개발구분 허용값. */
    private static final Set<String> DEV_TYPES = Set.of("신규", "유지", "삭제", "변경", "중복");
    /** 헤더 존: 멀티행 헤더(그룹+상세)를 수용하는 상위 행 수. */
    private static final int HEADER_ZONE = 12;

    static {
        REQUIRED.put(ArtifactType.PROGRAM_LIST, List.of("단위업무명", "프로그램 ID", "프로그램 명"));
        REQUIRED.put(ArtifactType.INTERFACE_DEFINITION, List.of("단위업무명", "인터페이스ID", "인터페이스명"));
        REQUIRED.put(ArtifactType.UI_LIST, List.of("단위업무명", "화면/보고서ID", "화면/보고서명"));
        REQUIRED.put(ArtifactType.REQUIREMENT_DEFINITION, List.of("요구사항 ID", "요구사항명"));

        ID_COLUMN.put(ArtifactType.BATCH_JOB_LIST, "Job ID");
        ID_COLUMN.put(ArtifactType.PROGRAM_LIST, "프로그램 ID");
        ID_COLUMN.put(ArtifactType.INTERFACE_DEFINITION, "인터페이스ID");
        ID_COLUMN.put(ArtifactType.UI_LIST, "화면/보고서ID");
        ID_COLUMN.put(ArtifactType.REQUIREMENT_DEFINITION, "요구사항 ID"); // spec §15.3 빈번사례: 요구사항 ID 중복
    }

    public List<Defect> validate(ParsedDocument document, ArtifactType type) {
        List<Defect> defects = new ArrayList<>();
        if (type == null || type == ArtifactType.UNKNOWN) {
            return defects;
        }
        String primaryToken = ID_COLUMN.getOrDefault(type,
                REQUIRED.containsKey(type) ? REQUIRED.get(type).get(0) : null);
        Map.Entry<String, List<List<String>>> body = selectBodySheet(document, primaryToken);
        if (body == null) {
            return defects;
        }
        String sheet = body.getKey();
        List<List<String>> rows = body.getValue();

        defects.addAll(checkRequired(type, sheet, rows));
        defects.addAll(checkIdDuplicates(type, sheet, rows));
        defects.addAll(checkDevTypeEnum(type, sheet, rows));
        return defects;
    }

    private List<Defect> checkRequired(ArtifactType type, String sheet, List<List<String>> rows) {
        List<Defect> defects = new ArrayList<>();
        List<String> required = REQUIRED.get(type);
        if (required == null) {
            return defects;
        }
        for (String col : required) {
            if (findInHeaderZone(rows, col) == null) {
                Defect d = base(Severity.IMPROVEMENT, DefectType.MISSING_REQUIRED, sheet);
                d.setChecklistItemKey(type.getChecklistKey() + ".required_column");
                d.setLocationColumn(col);
                d.setDescription("필수 컬럼 누락: '" + col + "' (" + type.getLabel() + ")");
                d.setImprovementGuide("본문에 '" + col + "' 컬럼을 추가하고 값을 기재하세요.");
                defects.add(d);
            }
        }
        return defects;
    }

    private List<Defect> checkIdDuplicates(ArtifactType type, String sheet, List<List<String>> rows) {
        List<Defect> defects = new ArrayList<>();
        String idToken = ID_COLUMN.get(type);
        if (idToken == null) {
            return defects;
        }
        int[] pos = findInHeaderZone(rows, idToken);
        if (pos == null) {
            return defects;
        }
        Set<String> seen = new LinkedHashSet<>();
        Set<String> reported = new LinkedHashSet<>();
        for (int r = pos[0] + 1; r < rows.size(); r++) {
            String value = cell(rows.get(r), pos[1]);
            if (value.isBlank()) {
                continue;
            }
            if (!seen.add(value) && reported.add(value)) {
                Defect d = base(Severity.IMPROVEMENT, DefectType.DUPLICATE, sheet);
                d.setChecklistItemKey(type.getChecklistKey() + ".id_duplicate");
                d.setLocationColumn(idToken);
                d.setLocationId(value);
                d.setDescription("ID 중복: '" + value + "' (" + idToken + " 중복)");
                d.setImprovementGuide("동일 ID가 중복되지 않도록 유일하게 부여하세요.");
                defects.add(d);
            }
        }
        return defects;
    }

    private List<Defect> checkDevTypeEnum(ArtifactType type, String sheet, List<List<String>> rows) {
        List<Defect> defects = new ArrayList<>();
        if (type != ArtifactType.BATCH_JOB_LIST) {
            return defects;
        }
        int[] pos = findInHeaderZone(rows, "개발구분");
        if (pos == null) {
            return defects;
        }
        for (int r = pos[0] + 1; r < rows.size(); r++) {
            String value = cell(rows.get(r), pos[1]);
            if (value.isBlank()) {
                continue;
            }
            if (!DEV_TYPES.contains(value.trim())) {
                Defect d = base(Severity.IMPROVEMENT, DefectType.STANDARD_VIOLATION, sheet);
                d.setChecklistItemKey("batch_job_list.dev_type_enum");
                d.setLocationColumn("개발구분");
                d.setLocationRow(String.valueOf(r));
                d.setDescription("개발구분 값 '" + value + "'은(는) 허용값(신규/유지/삭제/변경/중복)이 아닙니다.");
                d.setImprovementGuide("개발구분을 신규·유지·삭제·변경·중복 중 하나로 기재하세요.");
                defects.add(d);
            }
        }
        return defects;
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────
    private Defect base(Severity sev, DefectType type, String sheet) {
        Defect d = new Defect();
        d.setSeverity(sev);
        d.setDefectType(type);
        d.setPerspective(Perspective.ARTIFACT);
        d.setLocationSheet(sheet);
        return d;
    }

    /**
     * 본문 시트 선택: ① 헤더 존에 대표 토큰(ID열)이 있는 시트 → ② 이름(본문/양식/내용/목록/기능)
     * → ③ 최대 컬럼 수 시트. (신 포맷 AN06은 '기능' 시트가 본문)
     */
    private Map.Entry<String, List<List<String>>> selectBodySheet(ParsedDocument document, String primaryToken) {
        Map.Entry<String, List<List<String>>> byName = null;
        Map.Entry<String, List<List<String>>> byWidth = null;
        int maxWidth = -1;
        for (Map.Entry<String, List<List<String>>> e : document.getSheets().entrySet()) {
            if (primaryToken != null && findInHeaderZone(e.getValue(), primaryToken) != null) {
                return e;
            }
            String name = e.getKey() == null ? "" : e.getKey();
            if ((name.contains("본문") || name.contains("양식") || name.contains("내용")
                    || name.contains("목록") || name.contains("기능")) && byName == null) {
                byName = e;
            }
            int width = e.getValue().stream().mapToInt(List::size).max().orElse(0);
            if (width > maxWidth) {
                maxWidth = width;
                byWidth = e;
            }
        }
        return byName != null ? byName : byWidth;
    }

    /** 헤더 존(상위 12행)에서 토큰이 등장하는 첫 (행,열)을 찾는다. 멀티행 헤더 대응. 없으면 null. */
    private int[] findInHeaderZone(List<List<String>> rows, String token) {
        String target = normalize(token);
        int limit = Math.min(rows.size(), HEADER_ZONE);
        for (int r = 0; r < limit; r++) {
            List<String> row = rows.get(r);
            for (int c = 0; c < row.size(); c++) {
                String h = normalize(row.get(c));
                if (!h.isEmpty() && (h.equals(target) || h.contains(target))) {
                    return new int[]{r, c};
                }
            }
        }
        return null;
    }

    private String cell(List<String> row, int idx) {
        return idx >= 0 && idx < row.size() && row.get(idx) != null ? row.get(idx).trim() : "";
    }

    private String normalize(String s) {
        return s == null ? "" : s.replaceAll("\\s+", "").toLowerCase();
    }
}
