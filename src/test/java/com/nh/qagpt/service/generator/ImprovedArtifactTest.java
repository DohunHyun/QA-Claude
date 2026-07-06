package com.nh.qagpt.service.generator;

import com.nh.qagpt.domain.Defect;
import com.nh.qagpt.domain.ReviewResult;
import com.nh.qagpt.domain.enums.DefectType;
import com.nh.qagpt.domain.enums.Perspective;
import com.nh.qagpt.domain.enums.Severity;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** [S5] AI 개선 산출물 — 원본 구조 유지 + [개선] 태그(코멘트), 빈 항목 미기입, 권고 미반영. */
class ImprovedArtifactTest {

    private final ResultGeneratorImpl generator = new ResultGeneratorImpl();

    /** 본문 시트: 헤더[단위업무명, 배치Job ID], 데이터[여신, (빈칸)]. */
    private byte[] originalXlsx() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("본문");
            Row h = sheet.createRow(0);
            h.createCell(0).setCellValue("단위업무명");
            h.createCell(1).setCellValue("배치Job ID");
            Row r = sheet.createRow(1);
            r.createCell(0).setCellValue("여신");
            r.createCell(1).setCellValue(""); // 빈 항목
            wb.write(out);
            return out.toByteArray();
        }
    }

    private ReviewResult resultWith(Defect... defects) {
        ReviewResult result = new ReviewResult();
        for (Defect d : defects) {
            result.addDefect(d);
        }
        return result;
    }

    private Defect defect(Severity sev, String sheet, String column, String desc) {
        Defect d = new Defect();
        d.setSeverity(sev);
        d.setPerspective(Perspective.ARTIFACT);
        d.setDefectType(DefectType.MISSING_REQUIRED);
        d.setLocationSheet(sheet);
        d.setLocationColumn(column);
        d.setDescription(desc);
        d.setImprovementGuide("값을 기재하세요.");
        return d;
    }

    private String commentAt(Sheet sheet, int r, int c) {
        Row row = sheet.getRow(r);
        if (row == null || row.getCell(c) == null) {
            return null;
        }
        Comment cm = row.getCell(c).getCellComment();
        return cm == null ? null : cm.getString().getString();
    }

    @Test
    void 개선위치에_개선태그_코멘트_삽입_구조유지() throws Exception {
        ReviewResult result = resultWith(
                defect(Severity.IMPROVEMENT, "본문", "배치Job ID", "필수 컬럼 값 누락"));

        byte[] improved = generator.generateImprovedArtifact(result, originalXlsx(), "batch.xlsx");

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(improved))) {
            Sheet sheet = wb.getSheet("본문");
            assertThat(sheet).isNotNull(); // 시트 구조 유지
            // 원본 데이터 보존
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("단위업무명");
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("여신");
            // [개선] 태그가 배치Job ID 헤더 셀(0,1)에 코멘트로 표시
            assertThat(commentAt(sheet, 0, 1)).contains("[개선]").contains("필수 컬럼 값 누락");
        }
    }

    @Test
    void 빈항목은_임의로_채우지_않는다() throws Exception {
        ReviewResult result = resultWith(
                defect(Severity.IMPROVEMENT, "본문", "배치Job ID", "값 누락"));

        byte[] improved = generator.generateImprovedArtifact(result, originalXlsx(), "batch.xlsx");

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(improved))) {
            Cell empty = wb.getSheet("본문").getRow(1).getCell(1);
            // 빈 셀은 값이 채워지지 않음(코멘트만 가능, 값은 공백 유지)
            String v = empty == null ? "" : empty.getStringCellValue();
            assertThat(v).isEmpty();
        }
    }

    @Test
    void 권고항목은_개선본에_반영되지_않는다() throws Exception {
        ReviewResult result = resultWith(
                defect(Severity.RECOMMENDATION, "본문", "단위업무명", "권고 사항"));

        byte[] improved = generator.generateImprovedArtifact(result, originalXlsx(), "batch.xlsx");

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(improved))) {
            Sheet sheet = wb.getSheet("본문");
            assertThat(commentAt(sheet, 0, 0)).isNull(); // 권고는 태그 없음
        }
    }

    @Test
    void 비Excel포맷은_거부한다() throws Exception {
        ReviewResult result = resultWith(
                defect(Severity.IMPROVEMENT, "본문", "배치Job ID", "x"));
        assertThatThrownBy(() ->
                generator.generateImprovedArtifact(result, originalXlsx(), "지침서.hwpx"))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("Excel");
    }
}
