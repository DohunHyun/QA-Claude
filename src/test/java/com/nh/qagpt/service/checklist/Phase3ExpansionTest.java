package com.nh.qagpt.service.checklist;

import com.nh.qagpt.domain.Defect;
import com.nh.qagpt.domain.enums.ArtifactType;
import com.nh.qagpt.domain.enums.DefectType;
import com.nh.qagpt.service.parser.ParsedDocument;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** [P1-F] Phase3 유형 확대 — UI목록·요구사항정의서(멀티행 헤더 포함), 요약 추출 다중 ID 분리. */
class Phase3ExpansionTest {

    private final Phase3ListValidator validator = new Phase3ListValidator();
    private final ArtifactSummaryExtractor extractor = new ArtifactSummaryExtractor();

    private ParsedDocument doc(String sheetName, List<List<String>> rows) {
        ParsedDocument d = new ParsedDocument();
        d.getSheets().put(sheetName, rows);
        return d;
    }

    private boolean has(List<Defect> defects, DefectType type, String descPart) {
        return defects.stream().anyMatch(x -> x.getDefectType() == type
                && x.getDescription() != null && x.getDescription().contains(descPart));
    }

    @Test
    void UI목록_구템플릿_정상이면_결함없음() {
        // 실측: NHXXX DS01 본문 헤더
        List<Defect> defects = validator.validate(doc("본문", List.of(
                List.of("단위업무명", "LEVEL1", "화면/보고서ID", "화면/보고서명"),
                List.of("수신", "L1", "SCR-001", "계좌조회"))), ArtifactType.UI_LIST);
        assertThat(defects).isEmpty();
    }

    @Test
    void UI목록_ID컬럼누락_검출_그리고_ID중복_검출() {
        List<Defect> missing = validator.validate(doc("본문", List.of(
                List.of("단위업무명", "화면/보고서명"))), ArtifactType.UI_LIST);
        assertThat(has(missing, DefectType.MISSING_REQUIRED, "화면/보고서ID")).isTrue();

        List<Defect> dup = validator.validate(doc("본문", List.of(
                List.of("단위업무명", "화면/보고서ID", "화면/보고서명"),
                List.of("수신", "SCR-001", "a"),
                List.of("수신", "SCR-001", "b"))), ArtifactType.UI_LIST);
        assertThat(has(dup, DefectType.DUPLICATE, "SCR-001")).isTrue();
    }

    @Test
    void 요구사항정의서_신포맷_멀티행헤더_인식_결함없음() {
        // 실측: NHEFS AN06 '기능' 시트 — r0 그룹 헤더, r2 상세 헤더(요구사항ID 공백없음)
        List<Defect> defects = validator.validate(doc("기능", List.of(
                Arrays.asList("단위업무명", "Level 1", "최초 고객사 요건명세서", "", "요건 분석 및 협의완료된 사항"),
                Arrays.asList("", "", "", "", ""),
                Arrays.asList("", "", "최초요건번호", "최초요건명", "요구사항ID", "요구사항명"),
                Arrays.asList("AP/B2B", "AP", "4", "서류제출", "RQM-RF-EA-0001", "서류제출 요구사항"))),
                ArtifactType.REQUIREMENT_DEFINITION);
        assertThat(defects).isEmpty();
    }

    @Test
    void 요구사항ID_중복_검출_빈번사례() {
        // spec §15.3 빈번 시정조치 사례: 요구사항 ID 중복
        List<Defect> defects = validator.validate(doc("기능", List.of(
                Arrays.asList("요구사항 ID", "요구사항명"),
                Arrays.asList("RQM-1", "a"),
                Arrays.asList("RQM-1", "b"))), ArtifactType.REQUIREMENT_DEFINITION);
        assertThat(has(defects, DefectType.DUPLICATE, "RQM-1")).isTrue();
    }

    @Test
    void 추적표_요약추출_셀내_다중UID_분리() {
        // 실측: PM-310 AP&B2B — U ID 셀에 줄바꿈으로 여러 ID
        ParsedDocument d = doc("AP&B2B", List.of(
                Arrays.asList("최초요건", "", "분석", "", "설계"),
                Arrays.asList("최초요건번호", "요구사항 ID", "요구사항 명", "U ID", "UI 명"),
                Arrays.asList("4", "RQM-RF-EA-0001", "서류제출", "EFDSBAQ0I0\nEFDSBAQ0I1\nEFNT2064R0", "화면들"),
                Arrays.asList("5", "RQM-RF-EA-0002", "내역조회", "EFDSBAQ0R0", "조회화면")));
        ArtifactSummary s = extractor.extract(d, ArtifactType.REQUIREMENT_TRACEABILITY_MATRIX);
        assertThat(s.ids()).containsExactly("EFDSBAQ0I0", "EFDSBAQ0I1", "EFNT2064R0", "EFDSBAQ0R0");
    }

    @Test
    void UI목록_요약추출_화면보고서ID() {
        ParsedDocument d = doc("본문", List.of(
                Arrays.asList("단위업무명", "화면/보고서ID", "화면/보고서명"),
                Arrays.asList("수신", "EFDSBAQ0I0", "서류제출")));
        ArtifactSummary s = extractor.extract(d, ArtifactType.UI_LIST);
        assertThat(s.ids()).containsExactly("EFDSBAQ0I0");
    }
}
