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
 * [S8] Phase3 목록/정합성 검증 (spec §7.2 Phase3) — 산출물 유형 확대.
 *
 * 실측 컬럼(NHEFS 신 포맷) 기준:
 *  - 프로그램목록(DS07): 단위업무명·프로그램 ID·프로그램 명 (ID열=프로그램 ID)
 *  - 인터페이스정의서(AN08): 단위업무명·인터페이스ID·인터페이스명 (ID열=인터페이스ID)
 *  - 배치Job목록(AN07): 필수컬럼은 {@link ChecklistEngineImpl}가 담당, 여기선 ID중복·enum(개발구분)만.
 *
 * 공통: ID 중복(중복 결함), 목록형 필수컬럼 존재, 열거값 유효성.
 */
public class Phase3ListValidator {

    /** 유형별 목록형 필수컬럼 (배치Job목록은 엔진이 담당하므로 제외). */
    private static final Map<ArtifactType, List<String>> REQUIRED = new LinkedHashMap<>();
    /** 유형별 ID 컬럼(중복 검사 대상). */
    private static final Map<ArtifactType, String> ID_COLUMN = new LinkedHashMap<>();
    /** 배치Job목록 개발구분 허용값. */
    private static final Set<String> DEV_TYPES = Set.of("신규", "유지", "삭제", "변경", "중복");

    static {
        REQUIRED.put(ArtifactType.PROGRAM_LIST, List.of("단위업무명", "프로그램 ID", "프로그램 명"));
        REQUIRED.put(ArtifactType.INTERFACE_DEFINITION, List.of("단위업무명", "인터페이스ID", "인터페이스명"));

        ID_COLUMN.put(ArtifactType.BATCH_JOB_LIST, "Job ID");
        ID_COLUMN.put(ArtifactType.PROGRAM_LIST, "프로그램 ID");
        ID_COLUMN.put(ArtifactType.INTERFACE_DEFINITION, "인터페이스ID");
    }

    public List<Defect> validate(ParsedDocument document, ArtifactType type) {
        List<Defect> defects = new ArrayList<>();
        if (type == null || type == ArtifactType.UNKNOWN) {
            return defects;
        }
        Map.Entry<String, List<List<String>>> body = selectBodySheet(document);
        if (body == null) {
            return defects;
        }
        String sheet = body.getKey();
        List<List<String>> rows = body.getValue();
        int headerIdx = findHeaderRowIndex(rows);
        List<String> header = headerIdx < 0 ? List.of() : rows.get(headerIdx);

        defects.addAll(checkRequired(type, sheet, header));
        defects.addAll(checkIdDuplicates(type, sheet, rows, headerIdx, header));
        defects.addAll(checkDevTypeEnum(type, sheet, rows, headerIdx, header));
        return defects;
    }

    private List<Defect> checkRequired(ArtifactType type, String sheet, List<String> header) {
        List<Defect> defects = new ArrayList<>();
        List<String> required = REQUIRED.get(type);
        if (required == null) {
            return defects;
        }
        for (String col : required) {
            if (columnIndex(header, col) < 0) {
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

    private List<Defect> checkIdDuplicates(ArtifactType type, String sheet,
                                           List<List<String>> rows, int headerIdx, List<String> header) {
        List<Defect> defects = new ArrayList<>();
        String idToken = ID_COLUMN.get(type);
        if (idToken == null || headerIdx < 0) {
            return defects;
        }
        int idCol = columnIndex(header, idToken);
        if (idCol < 0) {
            return defects;
        }
        Set<String> seen = new LinkedHashSet<>();
        Set<String> reported = new LinkedHashSet<>();
        for (int r = headerIdx + 1; r < rows.size(); r++) {
            String value = cell(rows.get(r), idCol);
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

    private List<Defect> checkDevTypeEnum(ArtifactType type, String sheet,
                                          List<List<String>> rows, int headerIdx, List<String> header) {
        List<Defect> defects = new ArrayList<>();
        if (type != ArtifactType.BATCH_JOB_LIST || headerIdx < 0) {
            return defects;
        }
        int col = columnIndex(header, "개발구분");
        if (col < 0) {
            return defects;
        }
        for (int r = headerIdx + 1; r < rows.size(); r++) {
            String value = cell(rows.get(r), col);
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

    /** 본문 시트: 이름에 본문/양식/내용/목록 포함 우선, 없으면 최대 컬럼 수 시트. */
    private Map.Entry<String, List<List<String>>> selectBodySheet(ParsedDocument document) {
        Map.Entry<String, List<List<String>>> byName = null;
        Map.Entry<String, List<List<String>>> byWidth = null;
        int maxWidth = -1;
        for (Map.Entry<String, List<List<String>>> e : document.getSheets().entrySet()) {
            String name = e.getKey() == null ? "" : e.getKey();
            if (name.contains("본문") || name.contains("양식") || name.contains("내용") || name.contains("목록")) {
                if (byName == null) {
                    byName = e;
                }
            }
            int width = e.getValue().stream().mapToInt(List::size).max().orElse(0);
            if (width > maxWidth) {
                maxWidth = width;
                byWidth = e;
            }
        }
        return byName != null ? byName : byWidth;
    }

    private int findHeaderRowIndex(List<List<String>> rows) {
        int best = -1, maxNonEmpty = 0;
        int limit = Math.min(rows.size(), 12);
        for (int i = 0; i < limit; i++) {
            int nonEmpty = (int) rows.get(i).stream().filter(c -> c != null && !c.isBlank()).count();
            if (nonEmpty > maxNonEmpty) {
                maxNonEmpty = nonEmpty;
                best = i;
            }
        }
        return best;
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
