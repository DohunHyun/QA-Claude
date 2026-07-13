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
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetView;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetViews;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STSheetViewType;
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
            writeCoverSheet(wb, projectName, code, baseDate);
            writeRevisionSheet(wb, baseDate);
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

    // ── 표지 (PoC 표지 레이아웃 재현: 비밀구분 박스·큰 제목·문서정보 그리드) ──────────
    private void writeCoverSheet(Workbook wb, String projectName, String code, LocalDate baseDate) {
        Sheet sheet = wb.createSheet("표지");
        XSSFWorkbook xwb = (XSSFWorkbook) wb;
        // PoC 표지: 그리드 숨김(흰 배경) + 페이지나누기 미리보기 + 인쇄영역 A1:N17(파란 프레임)
        pageSetup(wb, sheet, true, 13, 16);

        sheet.setColumnWidth(0, (int) (8.8 * 256));
        sheet.setColumnWidth(14, (int) (8.8 * 256));   // O
        double[] heights = {24, 18.95, 24, 24, 24, 24, 75.75, 24, 24, 18.95, 18.95, 18.95, 18.95, 18.95, 18.95, 24, 24};
        for (int i = 0; i < heights.length; i++) {
            sheet.createRow(i).setHeightInPoints((float) heights[i]);
        }

        XSSFCellStyle secretLabel = coverStyle(xwb, 12, true, null, null, HorizontalAlignment.CENTER, BorderStyle.THIN);
        XSSFCellStyle secretValue = coverStyle(xwb, 12, true, "FF0000", null, HorizontalAlignment.CENTER, BorderStyle.THIN);
        XSSFCellStyle projName = coverStyle(xwb, 18, true, null, null, HorizontalAlignment.RIGHT, null);
        setSides(projName, null, BorderStyle.DOUBLE, null, null);            // 아래 이중선
        XSSFCellStyle title = coverStyle(xwb, 28, false, null, null, HorizontalAlignment.CENTER, null);
        setSides(title, BorderStyle.DOUBLE, BorderStyle.DOUBLE, null, null); // 위·아래 이중선
        XSSFCellStyle label = coverStyle(xwb, 12, true, null, "D9E1F2", HorizontalAlignment.CENTER, BorderStyle.THIN);
        XSSFCellStyle value = coverStyle(xwb, 12, true, null, null, HorizontalAlignment.CENTER, BorderStyle.THIN);
        XSSFCellStyle valueText = coverStyle(xwb, 12, true, null, null, HorizontalAlignment.CENTER, BorderStyle.THIN);
        valueText.setDataFormat(wb.createDataFormat().getFormat("@")); // 버전 텍스트 강제(부동소수점 방지)

        // 우상단 비밀구분 박스
        mergedCell(sheet, 2, 12, 13, "비밀구분", secretLabel);
        mergedCell(sheet, 3, 12, 13, "대외비", secretValue);
        // 프로젝트명 · 큰 제목
        mergedCell(sheet, 5, 0, 13, projectName, projName);
        mergedCell(sheet, 6, 0, 13, "시정조치서", title);
        // 문서정보 그리드 (라벨/값)
        mergedCell(sheet, 9, 4, 5, "문서번호", label);
        mergedCell(sheet, 9, 6, 9, code + "-PM-342-03", value);
        mergedCell(sheet, 10, 4, 5, "버전", label);
        mergedCell(sheet, 10, 6, 9, "1.1", valueText);
        mergedCell(sheet, 11, 4, 5, "제/개정일자", label);
        mergedCell(sheet, 11, 6, 9, baseDate == null ? "" : baseDate.format(DOT), value);
    }

    // ── 개정이력 (PoC 레이아웃 재현: 제목·회색 헤더·양식행·각주) ────────────────
    private void writeRevisionSheet(Workbook wb, LocalDate baseDate) {
        Sheet sheet = wb.createSheet("개정이력");
        XSSFWorkbook xwb = (XSSFWorkbook) wb;
        pageSetup(wb, sheet, true, null, null);   // 그리드 숨김 + 페이지나누기 미리보기(PoC 동일)

        double[] w = {10.6, 12, 10.6, 72.6, 10.6, 10.6};
        for (int c = 0; c < w.length; c++) {
            sheet.setColumnWidth(c, (int) (w[c] * 256));
        }

        XSSFCellStyle titleStyle = coverStyle(xwb, 14, true, null, null, HorizontalAlignment.CENTER, null);
        setSides(titleStyle, null, BorderStyle.THIN, null, null);
        XSSFCellStyle head = coverStyle(xwb, 11, true, null, "D9D9D9", HorizontalAlignment.CENTER, BorderStyle.THIN);
        XSSFCellStyle cellC = coverStyle(xwb, 10, false, null, null, HorizontalAlignment.CENTER, BorderStyle.THIN);
        XSSFCellStyle cellL = coverStyle(xwb, 10, false, null, null, HorizontalAlignment.LEFT, BorderStyle.THIN);
        XSSFCellStyle verCell = coverStyle(xwb, 10, false, null, null, HorizontalAlignment.CENTER, BorderStyle.THIN);
        verCell.setDataFormat(wb.createDataFormat().getFormat("@"));
        XSSFCellStyle note = coverStyle(xwb, 8, false, null, null, HorizontalAlignment.LEFT, null);
        note.setWrapText(false);

        sheet.createRow(0).setHeightInPoints(24);
        mergedCell(sheet, 0, 0, 5, "개정이력", titleStyle);

        String[] header = {"버전", "변경일", "구분", "개정내용", "작성자", "승인자"};
        Row hr = sheet.createRow(1);
        hr.setHeightInPoints(24);
        for (int c = 0; c < header.length; c++) {
            setStyled(hr, c, header[c], head);
        }

        Row dr = sheet.createRow(2);
        dr.setHeightInPoints(24);
        setStyled(dr, 0, "1.1", verCell);
        setStyled(dr, 1, baseDate == null ? "" : baseDate.format(DOT), cellC);
        setStyled(dr, 2, "최초 작성", cellC);
        setStyled(dr, 3, "AI 자동 생성 (품질검토 결과 반영)", cellL);
        setStyled(dr, 4, REVIEWER, cellC);
        setStyled(dr, 5, "", cellC);

        // 빈 양식행(향후 개정 기입용) — 그리드만
        for (int r = 3; r <= 17; r++) {
            Row er = sheet.createRow(r);
            er.setHeightInPoints(24);
            for (int c = 0; c < 6; c++) {
                setStyled(er, c, "", cellC);
            }
        }

        // 각주
        setStyled(sheet.createRow(18), 0, "· 구분: 변경 내용이 이전 문서에 대해 최초작성/추가/수정/삭제 중 선택 기입", note);
        setStyled(sheet.createRow(19), 0, "· 개정내용: 변경이 발생되는 위치와 변경 내용을 자세히 기록", note);
    }

    // ── 시정조치서 ─────────────────────────────────────────────────
    // 화면 시정조치관리대장과 동일한 색 구획을 재현한다:
    //   부적합사항=파랑(E9EEFC) · 시정조치 계획=주황(FDF1E4) · 시정조치 확인=초록(EAF6EE),
    //   전 셀 테두리·컬럼헤더 볼드·본문 줄바꿈. (셀 값은 종전과 동일 → 기존 스키마 검증 유지)
    private void writeActionSheet(Workbook wb, List<CorrectiveAction> actions,
                                  String stageLabel, LocalDate baseDate) {
        Sheet sheet = wb.createSheet("시정조치서");
        XSSFWorkbook xwb = (XSSFWorkbook) wb;
        pageSetup(wb, sheet, false, null, null);   // 그리드 숨김(흰 배경). 표는 자체 테두리 보유

        int target = actions.size();
        int done = (int) actions.stream().filter(a -> a.getStatus() == ActionStatus.DONE).count();
        int remaining = target - done; // [spec §4.4] 완료건수 = 조치 상태(DONE) 실집계

        CellStyle groupBlue = groupHeaderStyle(xwb, "E9EEFC", "284B8A");    // 부적합사항
        CellStyle groupOrange = groupHeaderStyle(xwb, "FDF1E4", "8A5A1E");  // 시정조치 계획
        CellStyle groupGreen = groupHeaderStyle(xwb, "EAF6EE", "1F7A43");   // 시정조치 확인
        CellStyle colHeader = columnHeaderStyle(xwb);
        CellStyle bodyCell = bodyStyle(xwb);
        CellStyle labelCell = summaryLabelStyle(xwb);

        // r0: 요약행 (PoC 셀 위치 재현)
        Row summary = sheet.createRow(0);
        setStyled(summary, 0, "단계", labelCell);
        setStyled(summary, 1, stageLabel, bodyCell);
        setStyled(summary, 2, "시정조치대상건수", labelCell);
        setStyled(summary, 4, String.valueOf(target), bodyCell);
        setStyled(summary, 6, "시정조치완료건수", labelCell);
        setStyled(summary, 7, String.valueOf(done), bodyCell);
        setStyled(summary, 10, "시정조치잔여건수", labelCell);
        setStyled(summary, 11, String.valueOf(remaining), bodyCell);
        setStyled(summary, 14, "산출물기준일", labelCell);
        setStyled(summary, 15, baseDate == null ? "" : baseDate.format(DOT), bodyCell);

        // r1: 그룹헤더 — 색 구획 3개(병합)
        Row group = sheet.createRow(1);
        fillGroup(group, 0, 10, "부적합사항", groupBlue);
        fillGroup(group, 11, 14, "시정조치 계획", groupOrange);
        fillGroup(group, 15, 16, "시정조치 확인", groupGreen);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 10));
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 11, 14));
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 15, 16));

        // r2: 컬럼 헤더 (17)
        Row header = sheet.createRow(2);
        header.setHeightInPoints(28);
        for (int c = 0; c < COLUMNS.length; c++) {
            setStyled(header, c, COLUMNS[c], colHeader);
        }

        // r3+: 데이터 — 영속 시정조치 라인(조치 계획/확인 상태 포함)
        int r = 3;
        for (CorrectiveAction a : actions) {
            Row row = sheet.createRow(r++);
            String[] v = rowValues(a);
            for (int c = 0; c < v.length; c++) {
                setStyled(row, c, v[c], bodyCell);
            }
        }

        // 열 너비 — 화면 가독성 반영(문자 수 기준, 1문자≈256)
        int[] widths = {8, 12, 7, 11, 12, 18, 34, 26, 10, 12, 42, 11, 11, 11, 26, 11, 11};
        for (int c = 0; c < widths.length; c++) {
            sheet.setColumnWidth(c, widths[c] * 256);
        }
    }

    /** 시정조치서 데이터행 17열 값(종전 순서·서식 그대로). */
    private String[] rowValues(CorrectiveAction a) {
        return new String[] {
                nullToEmpty(a.getNo()),
                nullToEmpty(a.getBusinessName()),
                "공통",
                a.getReviewDate() == null ? "" : a.getReviewDate().format(SHORT),
                nullToEmpty(a.getReviewer()),
                nullToEmpty(a.getArtifactName()),
                nullToEmpty(a.getFileName()),
                nullToEmpty(a.getNonconformityLocation()),
                a.getImprovementType() == null ? "" : a.getImprovementType().getLabel(),
                a.getDefectType() == null ? "" : a.getDefectType().getLabel(),
                nullToEmpty(a.getNonconformityContent()),
                nullToEmpty(a.getAssignee()),                    // 시정조치 계획(11~14)
                nullToEmpty(a.getPlannedDate()),
                a.getStatus() == ActionStatus.DONE && a.getConfirmedDate() != null
                        ? a.getConfirmedDate().format(SHORT) : "",
                nullToEmpty(a.getActionPlan()),
                nullToEmpty(a.getConfirmation()),                // 시정조치 확인(15~16)
                a.getConfirmedDate() == null ? "" : a.getConfirmedDate().format(SHORT)
        };
    }

    // ── 시정조치서 셀 스타일 (화면 색상 반영) ───────────────────────
    private void setStyled(Row row, int col, String value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value == null ? "" : value);
        c.setCellStyle(style);
    }

    /** 병합 그룹 구획: 영역 전 셀에 배경·테두리 적용(외곽선 렌더 보장), 첫 셀에 라벨. */
    private void fillGroup(Row row, int from, int to, String label, CellStyle style) {
        for (int c = from; c <= to; c++) {
            row.createCell(c).setCellStyle(style);
        }
        row.getCell(from).setCellValue(label);
    }

    /** 병합 셀: 영역 전 셀에 스타일(테두리 렌더 보장), 첫 셀에 값. c2==c1 이면 병합 없이 단일 셀. */
    private void mergedCell(Sheet sheet, int rowIdx, int c1, int c2, String value, CellStyle style) {
        Row r = sheet.getRow(rowIdx);
        if (r == null) {
            r = sheet.createRow(rowIdx);
        }
        for (int c = c1; c <= c2; c++) {
            r.createCell(c).setCellStyle(style);
        }
        r.getCell(c1).setCellValue(value == null ? "" : value);
        if (c2 > c1) {
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, c1, c2));
        }
    }

    /** 표지/개정이력 공통 셀 스타일 빌더. fontHex/fillHex/border 는 null 허용. */
    private XSSFCellStyle coverStyle(XSSFWorkbook wb, double size, boolean bold, String fontHex,
                                     String fillHex, HorizontalAlignment h, BorderStyle border) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(bold);
        f.setFontHeightInPoints((short) size);
        if (fontHex != null) {
            f.setColor(hex(fontHex));
        }
        s.setFont(f);
        if (fillHex != null) {
            s.setFillForegroundColor(hex(fillHex));
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        s.setAlignment(h);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setWrapText(true);
        if (border != null) {
            s.setBorderTop(border);
            s.setBorderBottom(border);
            s.setBorderLeft(border);
            s.setBorderRight(border);
        }
        return s;
    }

    /**
     * 시트 표시 설정 — 그리드선 숨김(흰 배경). previewFrame=true 면 페이지나누기 미리보기로 전환해
     * 인쇄영역 프레임을 노출한다. printCols/printRows(0-based) 지정 시 인쇄영역을 A1:(col,row)로 설정.
     */
    private void pageSetup(Workbook wb, Sheet sheet, boolean previewFrame, Integer printCols, Integer printRows) {
        sheet.setDisplayGridlines(false);
        sheet.setPrintGridlines(false);
        if (previewFrame) {
            CTSheetViews views = ((XSSFSheet) sheet).getCTWorksheet().getSheetViews();
            CTSheetView v = views.sizeOfSheetViewArray() > 0 ? views.getSheetViewArray(0) : views.addNewSheetView();
            v.setView(STSheetViewType.PAGE_BREAK_PREVIEW);
            v.setZoomScaleSheetLayoutView(100);
        }
        if (printCols != null && printRows != null) {
            wb.setPrintArea(wb.getSheetIndex(sheet), 0, printCols, 0, printRows);
        }
    }

    /** 특정 변(邊)만 테두리 지정(null=변경 없음). 이중선 등 부분 테두리용. */
    private void setSides(XSSFCellStyle s, BorderStyle top, BorderStyle bottom, BorderStyle left, BorderStyle right) {
        if (top != null) {
            s.setBorderTop(top);
        }
        if (bottom != null) {
            s.setBorderBottom(bottom);
        }
        if (left != null) {
            s.setBorderLeft(left);
        }
        if (right != null) {
            s.setBorderRight(right);
        }
    }

    private CellStyle groupHeaderStyle(XSSFWorkbook wb, String bgHex, String fgHex) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(hex(bgHex));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setColor(hex(fgHex));
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        thinBorder(s);
        return s;
    }

    private CellStyle columnHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(hex("F2F5FB"));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont f = wb.createFont();
        f.setBold(true);
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setWrapText(true);
        thinBorder(s);
        return s;
    }

    private CellStyle bodyStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setVerticalAlignment(VerticalAlignment.TOP);
        s.setWrapText(true);
        thinBorder(s);
        return s;
    }

    private CellStyle summaryLabelStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(hex("F2F5FB"));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont f = wb.createFont();
        f.setBold(true);
        s.setFont(f);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        thinBorder(s);
        return s;
    }

    private void thinBorder(XSSFCellStyle s) {
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
    }

    /** "RRGGBB" 16진수 → XSSFColor. */
    private XSSFColor hex(String rgb) {
        int r = Integer.parseInt(rgb.substring(0, 2), 16);
        int g = Integer.parseInt(rgb.substring(2, 4), 16);
        int b = Integer.parseInt(rgb.substring(4, 6), 16);
        return new XSSFColor(new byte[] {(byte) r, (byte) g, (byte) b}, null);
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

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
