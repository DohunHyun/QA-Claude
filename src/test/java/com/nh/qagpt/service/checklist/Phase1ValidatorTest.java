package com.nh.qagpt.service.checklist;

import com.nh.qagpt.domain.Defect;
import com.nh.qagpt.domain.Project;
import com.nh.qagpt.domain.enums.DefectType;
import com.nh.qagpt.domain.enums.Severity;
import com.nh.qagpt.service.parser.ParsedDocument;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** [S3] Phase1 표준 보증 검증 — 파일명/표지/개정이력 (프로젝트 등록정보 기준). */
class Phase1ValidatorTest {

    private final Phase1Validator validator = new Phase1Validator();

    private Project project() {
        Project p = new Project();
        p.setName("테스트검증프로젝트");
        p.setCode("NHEFS");
        p.setAnalysisStart(LocalDate.of(2025, 1, 1));
        p.setAnalysisEnd(LocalDate.of(2025, 12, 31));
        return p;
    }

    /** 표지/개정이력이 정상이고 파일명이 규칙을 지키면 파일명 관련 결함 없음. */
    private ParsedDocument goodDoc(String fileName) {
        ParsedDocument d = new ParsedDocument();
        d.setFileName(fileName);
        d.getSheets().put("표지", List.of(
                List.of("프로젝트명", "테스트검증프로젝트"),
                List.of("문서번호", "NHEFS-EA-AN07"),
                List.of("제·개정일자", "2025-05-19")));
        d.getSheets().put("개정이력", List.of(
                List.of("버전", "변경일", "작성자"),
                List.of("1.0", "2025-05-19", "홍길동")));
        return d;
    }

    private boolean has(List<Defect> defects, String descPart) {
        return defects.stream().anyMatch(x -> x.getDescription() != null && x.getDescription().contains(descPart));
    }

    @Test
    void 정상파일_정상표지_결함없음() {
        List<Defect> defects = validator.validate(
                goodDoc("NHEFS-EA-AN07-배치Job목록_V1.0_20250519.xlsx"), project());
        assertThat(defects).isEmpty();
    }

    @Test
    void 프로젝트_미등록시_Phase1_건너뜀() {
        assertThat(validator.validate(goodDoc("아무거나.xlsx"), null)).isEmpty();
    }

    @Test
    void 작성일자_누락_개선검출() {
        List<Defect> defects = validator.validate(
                goodDoc("NHEFS-EA-AN07-배치Job목록_V1.0.xlsx"), project());
        assertThat(has(defects, "작성일자(YYYYMMDD)")).isTrue();
        assertThat(defects.stream().anyMatch(x -> x.getSeverity() == Severity.IMPROVEMENT
                && x.getDefectType() == DefectType.MISSING_REQUIRED)).isTrue();
    }

    @Test
    void 분리자_하이픈오용_개선검출() {
        List<Defect> defects = validator.validate(
                goodDoc("NHEFS-EA-AN07-배치Job목록_V1.0-20250519.xlsx"), project());
        assertThat(has(defects, "구분자가 '-'")).isTrue();
    }

    @Test
    void 버전표기_소문자_개선검출() {
        List<Defect> defects = validator.validate(
                goodDoc("NHEFS-EA-AN07-배치Job목록_v1.0_20250519.xlsx"), project());
        assertThat(has(defects, "소문자 'v'")).isTrue();
    }

    @Test
    void 프로젝트코드_불일치_파일명_개선검출() {
        List<Defect> defects = validator.validate(
                goodDoc("NHXXX-EA-AN07-배치Job목록_V1.0_20250519.xlsx"), project());
        assertThat(has(defects, "프로젝트코드('NHEFS')")).isTrue();
    }

    @Test
    void 표지_제개정일자_기간밖_개선검출() {
        ParsedDocument d = new ParsedDocument();
        d.setFileName("NHEFS-EA-AN07-배치Job목록_V1.0_20240101.xlsx");
        d.getSheets().put("표지", List.of(
                List.of("문서번호", "NHEFS-EA-AN07"),
                List.of("프로젝트명", "테스트검증프로젝트"),
                List.of("제·개정일자", "2024-01-01"))); // 등록기간(2025) 밖
        d.getSheets().put("개정이력", List.of(List.of("1.0", "2024-01-01", "홍길동")));
        List<Defect> defects = validator.validate(d, project());
        assertThat(defects.stream().anyMatch(x -> x.getDescription().contains("등록 프로젝트 기간")
                && x.getSeverity() == Severity.IMPROVEMENT)).isTrue();
    }

    @Test
    void 표지_프로젝트코드_누락_개선검출() {
        ParsedDocument d = new ParsedDocument();
        d.setFileName("NHEFS-EA-AN07-배치Job목록_V1.0_20250519.xlsx");
        d.getSheets().put("표지", List.of(
                List.of("문서번호", "코드미기재"),
                List.of("제·개정일자", "2025-05-19")));
        d.getSheets().put("개정이력", List.of(List.of("1.0", "2025-05-19", "홍길동")));
        List<Defect> defects = validator.validate(d, project());
        assertThat(has(defects, "표지에 등록 프로젝트코드('NHEFS')")).isTrue();
    }

    @Test
    void 개정이력_시트없음_권고검출() {
        ParsedDocument d = new ParsedDocument();
        d.setFileName("NHEFS-EA-AN07-배치Job목록_V1.0_20250519.xlsx");
        d.getSheets().put("표지", List.of(
                List.of("문서번호", "NHEFS-EA-AN07"),
                List.of("프로젝트명", "테스트검증프로젝트"),
                List.of("제·개정일자", "2025-05-19")));
        List<Defect> defects = validator.validate(d, project());
        assertThat(defects.stream().anyMatch(x -> x.getDescription().contains("개정이력 시트")
                && x.getSeverity() == Severity.RECOMMENDATION)).isTrue();
    }
}
