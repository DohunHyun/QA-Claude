package com.nh.qagpt.service.generator;

import com.nh.qagpt.domain.CorrectiveAction;
import com.nh.qagpt.domain.Defect;
import com.nh.qagpt.domain.Document;
import com.nh.qagpt.domain.Project;
import com.nh.qagpt.domain.ReviewResult;
import com.nh.qagpt.domain.enums.ActionStatus;
import com.nh.qagpt.domain.enums.ArtifactType;
import com.nh.qagpt.domain.enums.Severity;
import com.nh.qagpt.repository.CorrectiveActionRepository;
import com.nh.qagpt.service.CorrectiveActionService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

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

    private final CorrectiveActionRepository correctiveActionRepository; // 널 허용(미영속 폴백)
    private final CorrectiveActionService correctiveActionService;

    public ResultGeneratorImpl(CorrectiveActionRepository correctiveActionRepository,
                               CorrectiveActionService correctiveActionService) {
        this.correctiveActionRepository = correctiveActionRepository;
        this.correctiveActionService = correctiveActionService;
    }

    @Override
    public byte[] generateImprovedArtifact(ReviewResult result, byte[] originalContent, String fileName) {
        String name = fileName == null ? "" : fileName.toLowerCase();
        if (!name.endsWith(".xlsx")) {
            // 원본 포맷 유지 원칙상 임의 변환 금지 — 현재 Excel(.xlsx)만 지원.
            throw new UnsupportedOperationException(
                    "개선 산출물 생성은 현재 Excel(.xlsx)만 지원합니다: " + fileName);
        }
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(originalContent));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            for (Defect d : result.getDefects()) {
                // 개선(ERROR) 항목만 변경 지점으로 표시 (권고는 개선본 미반영).
                if (d.getSeverity() != Severity.IMPROVEMENT) {
                    continue;
                }
                annotate(wb, d);
            }

            wb.write(out);
            return out.toByteArray();
        } catch (UnsupportedOperationException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("개선 산출물 생성 실패: " + e.getMessage(), e);
        }
    }

    /** 결함 위치 셀에 [개선] 태그 코멘트를 단다. 셀 값은 바꾸지 않아 구조·빈 항목을 그대로 유지한다. */
    private void annotate(Workbook wb, Defect d) {
        Sheet sheet = resolveSheet(wb, d.getLocationSheet());
        if (sheet == null) {
            return;
        }
        int[] rc = resolveCell(sheet, d);
        Row row = sheet.getRow(rc[0]);
        if (row == null) {
            row = sheet.createRow(rc[0]);
        }
        Cell cell = row.getCell(rc[1], Row.MissingCellPolicy.CREATE_NULL_AS_BLANK); // 값 미기입, 코멘트만
        String tag = "[개선] " + nullToEmpty(d.getDescription());
        if (d.getImprovementGuide() != null && !d.getImprovementGuide().isBlank()) {
            tag += "\n→ " + d.getImprovementGuide();
        }
        addOrAppendComment(wb, sheet, cell, rc[0], rc[1], tag);
    }

    private Sheet resolveSheet(Workbook wb, String sheetName) {
        if (sheetName != null && !sheetName.isBlank()) {
            Sheet exact = wb.getSheet(sheetName);
            if (exact != null) {
                return exact;
            }
            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                if (wb.getSheetName(i).contains(sheetName) || sheetName.contains(wb.getSheetName(i))) {
                    return wb.getSheetAt(i);
                }
            }
        }
        return wb.getNumberOfSheets() > 0 ? wb.getSheetAt(0) : null;
    }

    /** 위치 해석: 행 번호가 있으면 그 행, 없으면 컬럼명이 있는 헤더 셀, 둘 다 없으면 A1. */
    private int[] resolveCell(Sheet sheet, Defect d) {
        Integer rowIdx = parseRow(d.getLocationRow());
        String col = d.getLocationColumn();
        if (col != null && !col.isBlank()) {
            int headerRow = findHeaderRowContaining(sheet, col);
            if (headerRow >= 0) {
                int colIdx = findColumnIndex(sheet.getRow(headerRow), col);
                int r = rowIdx != null ? rowIdx : headerRow;
                return new int[]{r, Math.max(colIdx, 0)};
            }
        }
        return new int[]{rowIdx != null ? rowIdx : 0, 0};
    }

    private Integer parseRow(String s) {
        if (s == null) {
            return null;
        }
        String digits = s.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int findHeaderRowContaining(Sheet sheet, String col) {
        String target = col.replaceAll("\\s+", "");
        int limit = Math.min(sheet.getLastRowNum(), 10);
        for (int r = sheet.getFirstRowNum(); r <= limit; r++) {
            Row row = sheet.getRow(r);
            if (row != null && findColumnIndex(row, col) >= 0) {
                return r;
            }
        }
        return -1;
    }

    private int findColumnIndex(Row row, String col) {
        if (row == null) {
            return -1;
        }
        String target = col.replaceAll("\\s+", "");
        for (int c = 0; c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                String v = cell.getStringCellValue().replaceAll("\\s+", "");
                if (v.equals(target) || v.contains(target)) {
                    return c;
                }
            }
        }
        return -1;
    }

    private void addOrAppendComment(Workbook wb, Sheet sheet, Cell cell, int rowIdx, int colIdx, String text) {
        CreationHelper factory = wb.getCreationHelper();
        Comment existing = cell.getCellComment();
        if (existing != null) {
            String prev = existing.getString() == null ? "" : existing.getString().getString();
            existing.setString(factory.createRichTextString(prev + "\n" + text));
            return;
        }
        Drawing<?> drawing = sheet.createDrawingPatriarch();
        ClientAnchor anchor = factory.createClientAnchor();
        anchor.setCol1(colIdx);
        anchor.setCol2(colIdx + 3);
        anchor.setRow1(rowIdx);
        anchor.setRow2(rowIdx + 4);
        Comment comment = drawing.createCellComment(anchor);
        comment.setString(factory.createRichTextString(text));
        comment.setAuthor(REVIEWER);
        cell.setCellComment(comment);
    }

    @Override
    public byte[] generateCorrectiveActionLedger(ReviewResult result) {
        Project project = result.getProject();
        String stageLabel = result.getStage() == null ? "" : result.getStage().getLabel();
        return buildLedger(projectName(project), projectCode(project), stageLabel,
                baseDate(result), loadActions(result));
    }

    /**
     * 프로젝트 전체 시정조치관리대장 — 프로젝트의 모든 검토 회차 시정조치를 하나의 시정조치서 시트에
     * 집계한다(화면 시정조치관리대장과 동형: 단일 검토가 아닌 프로젝트 대장 전체).
     * 단계는 회차마다 다르므로 요약행 단계는 "전체", 기준일은 최신 검토일로 표기한다.
     */
    @Override
    public byte[] generateCorrectiveActionLedger(Project project, List<CorrectiveAction> actions) {
        LocalDate baseDate = actions.stream()
                .map(CorrectiveAction::getReviewDate)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
        return buildLedger(projectName(project), projectCode(project), "전체", baseDate, actions);
    }

    /** 3시트(표지·개정이력·시정조치서) 워크북을 조립한다. 검토 단위/프로젝트 단위 공통 경로. */
    private byte[] buildLedger(String projectName, String code, String stageLabel,
                               LocalDate baseDate, List<CorrectiveAction> actions) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle textStyle = wb.createCellStyle();
            textStyle.setDataFormat(wb.createDataFormat().getFormat("@")); // 텍스트 서식(부동소수점 방지)

            writeCoverSheet(wb, textStyle, projectName, code, baseDate);
            writeRevisionSheet(wb, textStyle, baseDate);
            writeActionSheet(wb, actions, stageLabel, baseDate);

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("시정조치관리대장 생성 실패: " + e.getMessage(), e);
        }
    }

    private String projectName(Project project) {
        return project == null || project.getName() == null ? "" : project.getName();
    }

    private String projectCode(Project project) {
        return project == null || project.getCode() == null ? "" : project.getCode();
    }

    /**
     * 대장 라인 로드 — 영속화된 시정조치 라인(조치 상태 반영) 우선, 미영속 결과(테스트 등)는
     * 결함으로부터 즉석 매핑(폴백).
     */
    private List<CorrectiveAction> loadActions(ReviewResult result) {
        if (correctiveActionRepository != null && result.getId() != null) {
            List<CorrectiveAction> persisted = correctiveActionRepository.findByReviewResultId(result.getId());
            if (!persisted.isEmpty()) {
                return persisted;
            }
        }
        return correctiveActionService.buildFromReview(result);
    }

    private final HwpxReviewReportWriter reportWriter = new HwpxReviewReportWriter();

    @Override
    public byte[] generateReviewReport(ReviewResult result) {
        return reportWriter.write(result);
    }

    // ── 표지 ──────────────────────────────────────────────────────
    private void writeCoverSheet(Workbook wb, CellStyle textStyle, String projectName, String code, LocalDate baseDate) {
        Sheet sheet = wb.createSheet("표지");

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
    private void writeActionSheet(Workbook wb, List<CorrectiveAction> actions,
                                  String stageLabel, LocalDate baseDate) {
        Sheet sheet = wb.createSheet("시정조치서");

        int target = actions.size();
        int done = (int) actions.stream().filter(a -> a.getStatus() == ActionStatus.DONE).count();
        int remaining = target - done; // [spec §4.4] 완료건수 = 조치 상태(DONE) 실집계

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

        // r3+: 데이터 — 영속 시정조치 라인(조치 계획/확인 상태 포함)
        int r = 3;
        for (CorrectiveAction a : actions) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(nullToEmpty(a.getNo()));
            row.createCell(1).setCellValue(nullToEmpty(a.getBusinessName()));
            row.createCell(2).setCellValue("공통");
            row.createCell(3).setCellValue(a.getReviewDate() == null ? "" : a.getReviewDate().format(SHORT));
            row.createCell(4).setCellValue(nullToEmpty(a.getReviewer()));
            row.createCell(5).setCellValue(nullToEmpty(a.getArtifactName()));
            row.createCell(6).setCellValue(nullToEmpty(a.getFileName()));
            row.createCell(7).setCellValue(nullToEmpty(a.getNonconformityLocation()));
            row.createCell(8).setCellValue(a.getImprovementType() == null ? "" : a.getImprovementType().getLabel());
            row.createCell(9).setCellValue(a.getDefectType() == null ? "" : a.getDefectType().getLabel());
            row.createCell(10).setCellValue(nullToEmpty(a.getNonconformityContent()));
            // 시정조치 계획(11~14)
            row.createCell(11).setCellValue(nullToEmpty(a.getAssignee()));
            row.createCell(12).setCellValue(nullToEmpty(a.getPlannedDate()));
            row.createCell(13).setCellValue(a.getStatus() == ActionStatus.DONE && a.getConfirmedDate() != null
                    ? a.getConfirmedDate().format(SHORT) : "");
            row.createCell(14).setCellValue(nullToEmpty(a.getActionPlan()));
            // 시정조치 확인(15~16)
            row.createCell(15).setCellValue(nullToEmpty(a.getConfirmation()));
            row.createCell(16).setCellValue(a.getConfirmedDate() == null ? "" : a.getConfirmedDate().format(SHORT));
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

    // (부적합 위치 문자열 매핑은 CorrectiveActionService.buildFromReview로 이동)

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
