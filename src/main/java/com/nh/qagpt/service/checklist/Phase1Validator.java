package com.nh.qagpt.service.checklist;

import com.nh.qagpt.domain.Defect;
import com.nh.qagpt.domain.Project;
import com.nh.qagpt.domain.enums.DefectType;
import com.nh.qagpt.domain.enums.Perspective;
import com.nh.qagpt.domain.enums.Severity;
import com.nh.qagpt.service.parser.ParsedDocument;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * [S3] Phase1 표준 보증 검증 (spec §4.1 명명규칙 2단계, §7.2 Phase1): 파일명 → 표지 → 개정이력.
 *
 * 명명규칙은 하드코딩하지 않는다: <b>1순위 = 등록 프로젝트정보</b>(코드·명·단계별 기간)를 기준값으로 사용한다.
 * (2순위 = 문서작성지침서 PM-141-01 파싱 보충은 후속 확장 — {@link ChecklistEngine} LLM 판정이 세부를 보완.)
 *
 * 프로젝트 미등록(project=null) 시 Phase1은 건너뛴다(기준값이 없으면 표준미준수 판정 불가).
 */
public class Phase1Validator {

    /** 파일명/셀 내 8자리 작성일자(YYYYMMDD). */
    private static final Pattern YYYYMMDD = Pattern.compile("(?<![0-9])(19|20)\\d{6}(?![0-9])");
    /** 구분자 뒤 버전 토큰: _V1.0 / -v1 등. group(1)=구분자, group(2)=V/v, group(3)=버전. */
    private static final Pattern VERSION = Pattern.compile("([_-])([vV])(\\d+(?:\\.\\d+)*)");
    /** yyyy.mm.dd / yyyy-mm-dd / yyyy/mm/dd (제·개정일자 셀). */
    private static final Pattern YMD = Pattern.compile("(\\d{4})[.\\-/](\\d{1,2})[.\\-/](\\d{1,2})");
    /** M/D/YY — POI DataFormatter가 날짜 셀을 미국식 단축형으로 렌더링하는 실측 케이스 (예: 5/19/25). */
    private static final Pattern US_SHORT = Pattern.compile("(?<![0-9])(\\d{1,2})/(\\d{1,2})/(\\d{2,4})(?![0-9])");
    /** yyyy년 m월 d일 (한국어 날짜 서식 셀). */
    private static final Pattern KO_YMD = Pattern.compile("(\\d{4})년\\s*(\\d{1,2})월\\s*(\\d{1,2})일");

    public List<Defect> validate(ParsedDocument document, Project project) {
        if (project == null) {
            return List.of();
        }
        List<Defect> defects = new ArrayList<>();
        defects.addAll(validateFileName(document.getFileName(), project));
        defects.addAll(validateCover(document, project));
        defects.addAll(validateRevision(document));
        return defects;
    }

    // ── 파일명 검증 ────────────────────────────────────────────────
    private List<Defect> validateFileName(String fileName, Project project) {
        List<Defect> defects = new ArrayList<>();
        if (fileName == null || fileName.isBlank()) {
            return defects;
        }

        // (1) 프로젝트코드 포함 여부
        String code = project.getCode();
        if (code != null && !code.isBlank()
                && !fileName.toUpperCase().contains(code.toUpperCase())) {
            defects.add(fileNameDefect(DefectType.STANDARD_VIOLATION, fileName,
                    "파일명에 등록 프로젝트코드('" + code + "')가 없습니다.",
                    "파일명을 'NH" + code + "-…' 규칙에 맞게 수정하세요."));
        }

        // (2) 작성일자(YYYYMMDD) 누락
        Matcher dateM = YYYYMMDD.matcher(fileName);
        boolean hasDate = dateM.find();
        if (!hasDate) {
            defects.add(fileNameDefect(DefectType.MISSING_REQUIRED, fileName,
                    "파일명에 작성일자(YYYYMMDD)가 없습니다.",
                    "파일명 끝에 '_YYYYMMDD'를 추가하세요."));
        } else {
            // (4) 작성일자 앞 구분자는 '_' (하이픈 오용 검출)
            int idx = dateM.start();
            if (idx > 0) {
                char sep = fileName.charAt(idx - 1);
                if (sep == '-') {
                    defects.add(fileNameDefect(DefectType.STANDARD_VIOLATION, fileName,
                            "작성일자 앞 구분자가 '-' 입니다(규칙: '_').",
                            "'…_V버전_YYYYMMDD' 형식으로 언더스코어를 사용하세요."));
                }
            }
        }

        // (3) 버전 표기 대소문자
        Matcher verM = VERSION.matcher(fileName);
        if (verM.find()) {
            if (verM.group(2).equals("v")) {
                defects.add(fileNameDefect(DefectType.STANDARD_VIOLATION, fileName,
                        "버전 표기가 소문자 'v'입니다(규칙: 대문자 'V').",
                        "'_V" + verM.group(3) + "' 처럼 대문자 V로 표기하세요."));
            }
        } else {
            defects.add(recommend(DefectType.STANDARD_VIOLATION, "phase1.filename.version",
                    "파일명에 버전 표기(_V버전)가 확인되지 않습니다.",
                    "'_V1.0' 형식의 버전 표기를 추가하세요.", locId(fileName)));
        }
        return defects;
    }

    // ── 표지 검증 ─────────────────────────────────────────────────
    private List<Defect> validateCover(ParsedDocument document, Project project) {
        List<Defect> defects = new ArrayList<>();
        Map.Entry<String, List<List<String>>> cover = findSheet(document, "표지");
        if (cover == null) {
            if (!document.getSheets().isEmpty()) {
                defects.add(recommend(DefectType.MISSING_REQUIRED, "phase1.cover.missing",
                        "표지 시트를 찾지 못했습니다.", "표준 템플릿의 표지 시트를 포함하세요.", null));
            }
            return defects;
        }
        String sheetName = cover.getKey();
        List<List<String>> rows = cover.getValue();

        // 프로젝트코드 일치
        String code = project.getCode();
        if (code != null && !code.isBlank() && !cellsContain(rows, code)) {
            defects.add(coverDefect(Severity.IMPROVEMENT, DefectType.STANDARD_VIOLATION, Perspective.PROCESS,
                    sheetName, "표지에 등록 프로젝트코드('" + code + "')가 확인되지 않습니다.",
                    "표지 문서번호/프로젝트코드를 등록 정보와 일치시키세요."));
        }
        // 프로젝트명 (표기 편차 가능성 → 권고)
        String name = project.getName();
        if (name != null && !name.isBlank() && !cellsContain(rows, name)) {
            defects.add(coverDefect(Severity.RECOMMENDATION, DefectType.CONTENT_ERROR, Perspective.PROCESS,
                    sheetName, "표지에서 등록 프로젝트명('" + name + "')을 확인하지 못했습니다.",
                    "표지 프로젝트명을 등록 정보와 일치시키세요."));
        }
        // 제·개정일자가 등록 기간 이내인지
        LocalDate coverDate = findCoverDate(rows);
        if (coverDate != null) {
            LocalDate start = earliestStart(project);
            LocalDate end = latestEnd(project);
            if ((start != null && coverDate.isBefore(start)) || (end != null && coverDate.isAfter(end))) {
                defects.add(coverDefect(Severity.IMPROVEMENT, DefectType.CONTENT_ERROR, Perspective.PROCESS,
                        sheetName, "표지 제·개정일자(" + coverDate + ")가 등록 프로젝트 기간(" + start + " ~ " + end + ") 밖입니다.",
                        "제·개정일자를 등록 프로젝트 기간 이내로 수정하거나 프로젝트 기간을 확인하세요."));
            }
        }
        return defects;
    }

    // ── 개정이력 검증 ─────────────────────────────────────────────
    private List<Defect> validateRevision(ParsedDocument document) {
        List<Defect> defects = new ArrayList<>();
        if (document.getSheets().isEmpty()) {
            return defects;
        }
        Map.Entry<String, List<List<String>>> rev = findSheet(document, "개정");
        if (rev == null) {
            defects.add(recommend(DefectType.MISSING_REQUIRED, "phase1.revision.missing",
                    "개정이력 시트를 찾지 못했습니다.", "표준 템플릿의 개정이력 시트를 포함하세요.", null));
            return defects;
        }
        boolean hasEntry = false;
        for (List<String> row : rev.getValue()) {
            boolean rowHasDate = row.stream().anyMatch(c -> c != null
                    && (YMD.matcher(c).find() || YYYYMMDD.matcher(c).find()
                        || US_SHORT.matcher(c).find() || KO_YMD.matcher(c).find()));
            boolean rowHasVersion = row.stream().anyMatch(c -> c != null && c.matches(".*\\d.*")
                    && (c.toLowerCase().contains("v") || c.matches("\\s*\\d+(\\.\\d+)*\\s*")));
            if (rowHasDate && rowHasVersion) {
                hasEntry = true;
                break;
            }
        }
        if (!hasEntry) {
            defects.add(recommend(DefectType.MISSING_REQUIRED, "phase1.revision.empty",
                    "개정이력에 유효한 이력 항목(버전+변경일)이 없습니다.",
                    "최초 작성 이력(버전/변경일/작성자)을 기재하세요.", rev.getKey()));
        }
        return defects;
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────
    private Defect fileNameDefect(DefectType type, String fileName, String desc, String guide) {
        Defect d = base(Severity.IMPROVEMENT, type, Perspective.ARTIFACT, desc, guide);
        d.setChecklistItemKey("phase1.filename");
        d.setLocationId(locId(fileName));
        return d;
    }

    private Defect coverDefect(Severity sev, DefectType type, Perspective persp,
                               String sheet, String desc, String guide) {
        Defect d = base(sev, type, persp, desc, guide);
        d.setChecklistItemKey("phase1.cover");
        d.setLocationSheet(sheet);
        return d;
    }

    private Defect recommend(DefectType type, String key, String desc, String guide, String sheet) {
        Defect d = base(Severity.RECOMMENDATION, type, Perspective.ARTIFACT, desc, guide);
        d.setChecklistItemKey(key);
        if (sheet != null) {
            d.setLocationSheet(sheet);
        }
        return d;
    }

    private Defect base(Severity sev, DefectType type, Perspective persp, String desc, String guide) {
        Defect d = new Defect();
        d.setSeverity(sev);
        d.setDefectType(type);
        d.setPerspective(persp);
        d.setDescription(desc);
        d.setImprovementGuide(guide);
        return d;
    }

    private static String locId(String fileName) {
        return "파일명:" + fileName;
    }

    private Map.Entry<String, List<List<String>>> findSheet(ParsedDocument doc, String keyword) {
        for (Map.Entry<String, List<List<String>>> e : doc.getSheets().entrySet()) {
            if (e.getKey() != null && e.getKey().contains(keyword)) {
                return e;
            }
        }
        return null;
    }

    private boolean cellsContain(List<List<String>> rows, String needle) {
        String target = needle.replaceAll("\\s+", "").toUpperCase();
        for (List<String> row : rows) {
            for (String cell : row) {
                if (cell != null && cell.replaceAll("\\s+", "").toUpperCase().contains(target)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 표지에서 '일자' 라벨 행의 날짜를 우선 사용, 없으면 표지 내 첫 파싱 가능한 날짜. */
    private LocalDate findCoverDate(List<List<String>> rows) {
        for (List<String> row : rows) {
            boolean labelRow = row.stream().anyMatch(c -> c != null && c.contains("일자"));
            if (labelRow) {
                for (String cell : row) {
                    LocalDate d = parseDate(cell);
                    if (d != null) {
                        return d;
                    }
                }
            }
        }
        for (List<String> row : rows) {
            for (String cell : row) {
                LocalDate d = parseDate(cell);
                if (d != null) {
                    return d;
                }
            }
        }
        return null;
    }

    private LocalDate parseDate(String cell) {
        if (cell == null || cell.isBlank()) {
            return null;
        }
        Matcher ymd = YMD.matcher(cell);
        if (ymd.find()) {
            try {
                return LocalDate.of(Integer.parseInt(ymd.group(1)),
                        Integer.parseInt(ymd.group(2)), Integer.parseInt(ymd.group(3)));
            } catch (RuntimeException ignored) {
                // 잘못된 일자 조합 → 무시
            }
        }
        Matcher ko = KO_YMD.matcher(cell);
        if (ko.find()) {
            try {
                return LocalDate.of(Integer.parseInt(ko.group(1)),
                        Integer.parseInt(ko.group(2)), Integer.parseInt(ko.group(3)));
            } catch (RuntimeException ignored) {
                // 무시
            }
        }
        Matcher eight = YYYYMMDD.matcher(cell);
        if (eight.find()) {
            String s = eight.group();
            try {
                return LocalDate.of(Integer.parseInt(s.substring(0, 4)),
                        Integer.parseInt(s.substring(4, 6)), Integer.parseInt(s.substring(6, 8)));
            } catch (RuntimeException ignored) {
                // 무시
            }
        }
        Matcher us = US_SHORT.matcher(cell);
        if (us.find()) { // POI 단축형 M/D/YY (연도 2자리는 2000년대로 해석)
            try {
                int year = Integer.parseInt(us.group(3));
                if (year < 100) {
                    year += 2000;
                }
                return LocalDate.of(year, Integer.parseInt(us.group(1)), Integer.parseInt(us.group(2)));
            } catch (RuntimeException ignored) {
                // 무시
            }
        }
        return null;
    }

    private LocalDate earliestStart(Project p) {
        return min(min(p.getManagementStart(), p.getAnalysisStart()), p.getDesignStart());
    }

    private LocalDate latestEnd(Project p) {
        return max(max(p.getManagementEnd(), p.getAnalysisEnd()), p.getDesignEnd());
    }

    private LocalDate min(LocalDate a, LocalDate b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isBefore(b) ? a : b;
    }

    private LocalDate max(LocalDate a, LocalDate b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isAfter(b) ? a : b;
    }
}
