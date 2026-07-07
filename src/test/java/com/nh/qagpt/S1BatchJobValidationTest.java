package com.nh.qagpt;

import com.nh.qagpt.domain.Defect;
import com.nh.qagpt.domain.enums.ArtifactType;
import com.nh.qagpt.domain.enums.DefectType;
import com.nh.qagpt.domain.enums.Severity;
import com.nh.qagpt.service.checklist.ChecklistEngineImpl;
import com.nh.qagpt.service.parser.ExcelDocumentParser;
import com.nh.qagpt.service.parser.ParsedDocument;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** [S1] 배치Job목록 파싱→필수컬럼 검증 파이프라인 로직 테스트. */
class S1BatchJobValidationTest {

    private final ExcelDocumentParser parser = new ExcelDocumentParser();
    private final ChecklistEngineImpl engine = new ChecklistEngineImpl();

    @Test
    void 필수컬럼_모두있으면_결함없음() throws Exception {
        // 구 템플릿 표기(배치Job ID / 업무명) 기준 정상 문서
        byte[] xlsx = workbook(List.of("단위업무명", "배치Job ID", "업무명"),
                List.of("여신", "BJ-DE-0001", "일일정산배치"));

        ParsedDocument parsed = parser.parse(xlsx, "batch.xlsx", null);
        List<Defect> defects = engine.apply(parsed, ArtifactType.BATCH_JOB_LIST, null);

        assertThat(defects).isEmpty();
    }

    @Test
    void 신포맷_영문JobID표기도_정상인식() throws Exception {
        // 신 포맷 표기(Batch Job ID) — "Job ID" 토큰 매칭으로 결함 없어야 함
        byte[] xlsx = workbook(List.of("단위업무명", "Batch Job ID", "Batch Job 설명"),
                List.of("AP", "befepcopr01", "비대면기업뱅킹신규진행만료처리"));

        ParsedDocument parsed = parser.parse(xlsx, "batch.xlsx", null);
        List<Defect> defects = engine.apply(parsed, ArtifactType.BATCH_JOB_LIST, null);

        assertThat(defects).isEmpty();
    }

    @Test
    void 단위업무명컬럼_누락시_개선결함_근거위치포함() throws Exception {
        byte[] xlsx = workbook(List.of("배치Job ID", "업무명"),
                List.of("BJ-DE-0001", "일일정산배치"));

        ParsedDocument parsed = parser.parse(xlsx, "batch.xlsx", null);
        List<Defect> defects = engine.apply(parsed, ArtifactType.BATCH_JOB_LIST, null);

        assertThat(defects).hasSize(1);
        Defect d = defects.get(0);
        assertThat(d.getSeverity()).isEqualTo(Severity.IMPROVEMENT);
        assertThat(d.getDefectType()).isEqualTo(DefectType.MISSING_REQUIRED);
        assertThat(d.getLocationColumn()).isEqualTo("단위업무명");
        assertThat(d.getLocationSheet()).isNotBlank();
    }

    private byte[] workbook(List<String> header, List<String> dataRow) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("본문");
            Row h = sheet.createRow(0);
            for (int i = 0; i < header.size(); i++) {
                h.createCell(i).setCellValue(header.get(i));
            }
            Row r = sheet.createRow(1);
            for (int i = 0; i < dataRow.size(); i++) {
                r.createCell(i).setCellValue(dataRow.get(i));
            }
            wb.write(out);
            return out.toByteArray();
        }
    }
}
