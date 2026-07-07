package com.nh.qagpt.service.generator;

import com.nh.qagpt.domain.Defect;
import com.nh.qagpt.domain.Document;
import com.nh.qagpt.domain.Project;
import com.nh.qagpt.domain.ReviewResult;
import com.nh.qagpt.domain.enums.ArtifactType;
import com.nh.qagpt.domain.enums.Perspective;
import com.nh.qagpt.domain.enums.Severity;
import com.nh.qagpt.domain.enums.Stage;
import com.nh.qagpt.service.parser.ParsedDocument;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 결과물 생성 (spec §4.4). [S4] 시정조치관리대장(.xlsx) 구현.
 *
 * PoC 실측 스키마 재현: 3시트(표지·개정이력·시정조치서), 시정조치서 = 요약행 + 그룹헤더 + 본문 17열
 * (부적합사항 11 + 시정조치 계획 4 + 확인 2). No=CA_P##/CA_W##, 검토자="AI품질검토봇".
 * 버전·식별번호 셀은 텍스트(@) 서식으로 강제해 POI 부동소수점(1.1→1.1000…) 오류를 막는다.
 */
@Service
public class ResultGeneratorImpl implements ResultGenerator {

    private static final String REVIEWER = "AI품질검토봇";
    private static final DateTimeFormatter DOT = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("yy.MM.dd");

    /** 시정조치서 본문 17열 헤더 (PoC 실측 순서). */
    private static final String[] COLUMNS = {
            "No", "업무명", "구분", "검토일", "검토자", "산출물명", "파일명", "부적합 위치",
            "개선 유형", "결함유형", "결함내용",
            "조치\n담당자", "완료\n예정일", "조치\n완료일", "비고(시정조치계획)",
            "조치\n확인자", "조치\n확인일"
    };

    @Override
    public byte[] generateImprovedArtifact(ReviewResult result, ParsedDocument original) {
        // TODO(S5): 원본 구조 유지한 채 개선(ERROR) 항목 수정 + [개선] 태그 삽입 (Apache POI / HWPX 파서).
        throw new UnsupportedOperationException("TODO(S5): AI 개선 산출물 생성");
    }

    @Override
    public byte[] generateCorrectiveActionLedger(ReviewResult result) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle textStyle = wb.createCellStyle();
            textStyle.setDataFormat(wb.createDataFormat().getFormat("@")); // 텍스트 서식(부동소수점 방지)

            LocalDate baseDate = baseDate(result);
            writeCoverSheet(wb, textStyle, result, baseDate);
            writeRevisionSheet(wb, textStyle, baseDate);
            writeActionSheet(wb, result, baseDate);

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("시정조치관리대장 생성 실패: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] generateReviewReport(ReviewResult result) {
        // TODO(S7): 단계말 검토결과서(HWPX) — 항목별 결과(개선/권고/OK)·결함유형·관점·근거 위치.
        throw new UnsupportedOperationException("TODO(S7): 단계말 검토결과서 생성");
    }

    // ── 표지 ──────────────────────────────────────────────────────
    private void writeCoverSheet(Workbook wb, CellStyle textStyle, ReviewResult result, LocalDate baseDate) {
        Sheet sheet = wb.createSheet("표지");
        Project project = result.getProject();
        String projectName = project == null || project.getName() == null ? "" : project.getName();
        String code = project == null || project.getCode() == null ? "" : project.getCode();

        setString(sheet, 0, 0, projectName);
        setString(sheet, 1, 0, "시정조치서");
        setLabelValue(sheet, 3, "문서번호", "NH" + code + "-PM-342-03");
        // 버전은 텍스트 강제 — 숫자 서식이면 1.1 → 1.1000000000000001 로 표기됨
        setLabelValueStyled(sheet, 4, "버전", "1.1", textStyle);
        setLabelValue(sheet, 5, "제/개정일자", baseDate == null ? "" : baseDate.format(DOT));
    }

    // ── 개정이력 ───────────────────────────────────────────────────
    private void writeRevisionSheet(Workbook wb, CellStyle textStyle, LocalDate baseDate) {
        Sheet sheet = wb.createSheet("개정이력");
        setString(sheet, 0, 0, "개정이력");
        String[] header = {"버전", "변경일", "구분", "개정내용", "작성자"};
        Row hr = sheet.createRow(1);
        for (int c = 0; c < header.length; c++) {
            hr.createCell(c).setCellValue(header[c]);
        }
        Row row = sheet.createRow(2);
        Cell ver = row.createCell(0);
        ver.setCellStyle(textStyle);
        ver.setCellValue("1.1");
        row.createCell(1).setCellValue(baseDate == null ? "" : baseDate.format(DOT));
        row.createCell(2).setCellValue("최초 작성");
        row.createCell(3).setCellValue("AI 자동 생성 (품질검토 결과 반영)");
        row.createCell(4).setCellValue(REVIEWER);
    }

    // ── 시정조치서 ─────────────────────────────────────────────────
    private void writeActionSheet(Workbook wb, ReviewResult result, LocalDate baseDate) {
        Sheet sheet = wb.createSheet("시정조치서");
        List<Defect> defects = result.getDefects();

        int target = defects.size();
        int done = 0; // 신규 생성 시점: 완료 0
        int remaining = target - done;
        String stageLabel = result.getStage() == null ? "" : result.getStage().getLabel();

        // r0: 요약행 (PoC 셀 위치 재현)
        Row summary = sheet.createRow(0);
        summary.createCell(0).setCellValue("단계");
        summary.createCell(1).setCellValue(stageLabel);
        summary.createCell(2).setCellValue("시정조치대상건수");
        summary.createCell(4).setCellValue(String.valueOf(target));
        summary.createCell(6).setCellValue("시정조치완료건수");
        summary.createCell(7).setCellValue(String.valueOf(done));
        summary.createCell(10).setCellValue("시정조치잔여건수");
        summary.createCell(11).setCellValue(String.valueOf(remaining));
        summary.createCell(14).setCellValue("산출물기준일");
        summary.createCell(15).setCellValue(baseDate == null ? "" : baseDate.format(DOT));

        // r1: 그룹헤더
        Row group = sheet.createRow(1);
        group.createCell(0).setCellValue("부적합사항");
        group.createCell(11).setCellValue("시정조치 계획");
        group.createCell(15).setCellValue("시정조치 확인");

        // r2: 컬럼 헤더 (17)
        Row header = sheet.createRow(2);
        for (int c = 0; c < COLUMNS.length; c++) {
            header.createCell(c).setCellValue(COLUMNS[c]);
        }

        // r3+: 데이터
        String reviewDate = baseDate == null ? "" : baseDate.format(SHORT);
        String artifactName = artifactLabel(result.getDocument());
        String fileName = result.getDocument() == null ? "" : nullToEmpty(result.getDocument().getFileName());

        int pSeq = 0, wSeq = 0;
        int r = 3;
        for (Defect d : defects) {
            boolean isProcess = d.getPerspective() == Perspective.PROCESS;
            String no = isProcess
                    ? String.format("CA_P%02d", ++pSeq)
                    : String.format("CA_W%02d", ++wSeq);
            String businessName = isProcess ? "프로젝트관리" : "개발산출물";

            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(no);
            row.createCell(1).setCellValue(businessName);
            row.createCell(2).setCellValue("공통");
            row.createCell(3).setCellValue(reviewDate);
            row.createCell(4).setCellValue(REVIEWER);
            row.createCell(5).setCellValue(artifactName);
            row.createCell(6).setCellValue(fileName);
            row.createCell(7).setCellValue(location(d));
            row.createCell(8).setCellValue(d.getSeverity() == null ? "" : d.getSeverity().getLabel());
            row.createCell(9).setCellValue(d.getDefectType() == null ? "" : d.getDefectType().getLabel());
            row.createCell(10).setCellValue(nullToEmpty(d.getDescription()));
            // 시정조치 계획(11~14): 개선 방향을 시정조치 계획 비고로, 담당/일자는 미정
            row.createCell(11).setCellValue("");
            row.createCell(12).setCellValue("");
            row.createCell(13).setCellValue("");
            row.createCell(14).setCellValue(nullToEmpty(d.getImprovementGuide()));
            // 시정조치 확인(15~16): 신규 → 미확인
            row.createCell(15).setCellValue("");
            row.createCell(16).setCellValue("");
        }
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────
    private LocalDate baseDate(ReviewResult result) {
        if (result.getCreatedAt() != null) {
            return result.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        return null;
    }

    private String artifactLabel(Document doc) {
        if (doc == null || doc.getArtifactType() == null || doc.getArtifactType() == ArtifactType.UNKNOWN) {
            return "";
        }
        return doc.getArtifactType().getLabel();
    }

    private String location(Defect d) {
        StringBuilder sb = new StringBuilder();
        appendLoc(sb, "시트", d.getLocationSheet());
        appendLoc(sb, "행", d.getLocationRow());
        appendLoc(sb, "열", d.getLocationColumn());
        appendLoc(sb, "ID", d.getLocationId());
        return sb.toString();
    }

    private void appendLoc(StringBuilder sb, String key, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(", ");
        }
        sb.append(key).append(":").append(value);
    }

    private void setString(Sheet sheet, int rowIdx, int colIdx, String value) {
        Row row = sheet.getRow(rowIdx);
        if (row == null) {
            row = sheet.createRow(rowIdx);
        }
        row.createCell(colIdx).setCellValue(value == null ? "" : value);
    }

    /** 라벨(col4)·값(col6) 배치는 표지 PoC 레이아웃을 단순화해 라벨=col0, 값=col1로 기록. */
    private void setLabelValue(Sheet sheet, int rowIdx, String label, String value) {
        Row row = sheet.createRow(rowIdx);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value == null ? "" : value);
    }

    private void setLabelValueStyled(Sheet sheet, int rowIdx, String label, String value, CellStyle valueStyle) {
        Row row = sheet.createRow(rowIdx);
        row.createCell(0).setCellValue(label);
        Cell v = row.createCell(1);
        v.setCellStyle(valueStyle);
        v.setCellValue(value == null ? "" : value);
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
