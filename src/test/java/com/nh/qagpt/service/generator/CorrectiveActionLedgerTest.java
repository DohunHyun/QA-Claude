package com.nh.qagpt.service.generator;

import com.nh.qagpt.domain.Defect;
import com.nh.qagpt.domain.Document;
import com.nh.qagpt.domain.Project;
import com.nh.qagpt.domain.ReviewResult;
import com.nh.qagpt.domain.enums.ArtifactType;
import com.nh.qagpt.domain.enums.DefectType;
import com.nh.qagpt.domain.enums.Perspective;
import com.nh.qagpt.domain.enums.Severity;
import com.nh.qagpt.domain.enums.Stage;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/** [S4] 시정조치관리대장 생성 — 3시트·17열·요약집계·CA_P/W·검토자·버전 텍스트 서식 검증. */
class CorrectiveActionLedgerTest {

    private final ResultGeneratorImpl generator = new ResultGeneratorImpl(null, new com.nh.qagpt.service.CorrectiveActionService(null));
    private final DataFormatter fmt = new DataFormatter();

    private ReviewResult sampleResult() {
        Project p = new Project();
        p.setName("테스트검증프로젝트");
        p.setCode("NHEFS");

        Document doc = new Document();
        doc.setFileName("NHEFS-EA-AN07-배치Job목록_V1.01.xlsx");
        doc.setArtifactType(ArtifactType.BATCH_JOB_LIST);

        ReviewResult result = new ReviewResult();
        result.setStage(Stage.ANALYSIS);
        result.setProject(p);
        result.setDocument(doc);

        Defect processDefect = new Defect();
        processDefect.setPerspective(Perspective.PROCESS);
        processDefect.setSeverity(Severity.IMPROVEMENT);
        processDefect.setDefectType(DefectType.STANDARD_VIOLATION);
        processDefect.setDescription("표지 프로젝트코드 불일치");
        processDefect.setLocationSheet("표지");
        result.addDefect(processDefect);

        Defect artifactDefect = new Defect();
        artifactDefect.setPerspective(Perspective.ARTIFACT);
        artifactDefect.setSeverity(Severity.RECOMMENDATION);
        artifactDefect.setDefectType(DefectType.MISSING_REQUIRED);
        artifactDefect.setDescription("필수 컬럼 누락");
        artifactDefect.setLocationSheet("본문");
        artifactDefect.setLocationColumn("단위업무명");
        result.addDefect(artifactDefect);

        return result;
    }

    private Workbook read(byte[] xlsx) throws Exception {
        return new XSSFWorkbook(new ByteArrayInputStream(xlsx));
    }

    private String cell(Sheet sheet, int r, int c) {
        Row row = sheet.getRow(r);
        return row == null || row.getCell(c) == null ? "" : fmt.formatCellValue(row.getCell(c));
    }

    @Test
    void 세시트와_17열_스키마로_생성된다() throws Exception {
        try (Workbook wb = read(generator.generateCorrectiveActionLedger(sampleResult()))) {
            assertThat(wb.getSheet("표지")).isNotNull();
            assertThat(wb.getSheet("개정이력")).isNotNull();
            Sheet action = wb.getSheet("시정조치서");
            assertThat(action).isNotNull();

            Row header = action.getRow(2);
            assertThat(header.getLastCellNum()).isEqualTo((short) 17);
            assertThat(cell(action, 2, 0)).isEqualTo("No");
            assertThat(cell(action, 2, 16)).contains("확인일");
        }
    }

    @Test
    void 요약건수가_자동집계된다() throws Exception {
        try (Workbook wb = read(generator.generateCorrectiveActionLedger(sampleResult()))) {
            Sheet action = wb.getSheet("시정조치서");
            assertThat(cell(action, 0, 0)).isEqualTo("단계");
            assertThat(cell(action, 0, 1)).isEqualTo("분석");
            assertThat(cell(action, 0, 4)).isEqualTo("2");  // 대상
            assertThat(cell(action, 0, 7)).isEqualTo("0");  // 완료
            assertThat(cell(action, 0, 11)).isEqualTo("2"); // 잔여
        }
    }

    @Test
    void No가_CA_P_CA_W규칙_검토자는_AI품질검토봇() throws Exception {
        try (Workbook wb = read(generator.generateCorrectiveActionLedger(sampleResult()))) {
            Sheet action = wb.getSheet("시정조치서");
            // r3 = 프로세스 관점 → CA_P01 / 업무명 프로젝트관리
            assertThat(cell(action, 3, 0)).isEqualTo("CA_P01");
            assertThat(cell(action, 3, 1)).isEqualTo("프로젝트관리");
            assertThat(cell(action, 3, 4)).isEqualTo("AI품질검토봇");
            // r4 = 산출물 관점 → CA_W01 / 업무명 개발산출물
            assertThat(cell(action, 4, 0)).isEqualTo("CA_W01");
            assertThat(cell(action, 4, 1)).isEqualTo("개발산출물");
            assertThat(cell(action, 4, 6)).isEqualTo("NHEFS-EA-AN07-배치Job목록_V1.01.xlsx");
            assertThat(cell(action, 4, 8)).isEqualTo("권고");
            assertThat(cell(action, 4, 9)).isEqualTo("필수항목누락");
        }
    }

    @Test
    void 버전값이_부동소수점오차없이_텍스트로_표기된다() throws Exception {
        try (Workbook wb = read(generator.generateCorrectiveActionLedger(sampleResult()))) {
            // 표지 버전 셀
            Sheet cover = wb.getSheet("표지");
            String coverVer = null;
            for (Row row : cover) {
                if (row.getCell(0) != null && "버전".equals(fmt.formatCellValue(row.getCell(0)))) {
                    coverVer = fmt.formatCellValue(row.getCell(1));
                }
            }
            assertThat(coverVer).isEqualTo("1.1").doesNotContain("1.1000");
            // 개정이력 버전 셀
            assertThat(cell(wb.getSheet("개정이력"), 2, 0)).isEqualTo("1.1").doesNotContain("1.1000");
        }
    }

    @Test
    void 결함없으면_대상0건_요약() throws Exception {
        ReviewResult empty = new ReviewResult();
        empty.setStage(Stage.DESIGN);
        try (Workbook wb = read(generator.generateCorrectiveActionLedger(empty))) {
            Sheet action = wb.getSheet("시정조치서");
            assertThat(cell(action, 0, 4)).isEqualTo("0");
            assertThat(cell(action, 0, 11)).isEqualTo("0");
            assertThat(action.getRow(3)).isNull(); // 데이터 행 없음
        }
    }
}
