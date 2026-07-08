package com.nh.qagpt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [P1-E] 시정조치 영속화 라이프사이클 E2E (spec §4.4·§8.2):
 * 검증 → 라인 자동 생성(TARGET) → 상태 갱신(DONE) → 대장 재발급 시 완료/잔여 실집계 반영.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CorrectiveActionLifecycleApiTest {

    private static final String XLSX_MIME =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    private final DataFormatter fmt = new DataFormatter();

    /** 필수컬럼(단위업무명) 누락 배치Job목록 → 개선 결함 1건. */
    private byte[] defectiveXlsx() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("본문");
            Row h = sheet.createRow(0);
            h.createCell(0).setCellValue("배치Job ID");
            wb.write(out);
            return out.toByteArray();
        }
    }

    @Test
    void 검증후_라인생성_DONE전이_대장_완료건수_실집계() throws Exception {
        // 1) 검증 (결함 1건)
        MvcResult res = mvc.perform(multipart("/api/validate")
                        .file(new MockMultipartFile("file", "batch.xlsx", XLSX_MIME, defectiveXlsx()))
                        .param("artifactType", "BATCH_JOB_LIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passed").value(false))
                .andReturn();
        long reviewId = mapper.readTree(res.getResponse().getContentAsString()).get("reviewId").asLong();

        // 2) 시정조치 라인 자동 생성 확인 (No=CA_W01, 상태 TARGET)
        MvcResult listRes = mvc.perform(get("/api/reviews/{id}/corrective-actions", reviewId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].no").value("CA_W01"))
                .andExpect(jsonPath("$[0].status").value("TARGET"))
                .andExpect(jsonPath("$[0].improvementType").value("개선"))
                .andReturn();
        long actionId = mapper.readTree(listRes.getResponse().getContentAsString()).get(0).get("id").asLong();

        // 3) 조치 완료 전이 (담당자·확인 기록)
        mvc.perform(patch("/api/corrective-actions/{id}", actionId)
                        .param("status", "DONE")
                        .param("assignee", "김PM")
                        .param("confirmation", "컬럼 추가 완료"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.assignee").value("김PM"))
                .andExpect(jsonPath("$.confirmedDate").isNotEmpty());

        // 4) 대장 재발급 → 완료 1 / 잔여 0 실집계 + 조치 필드 반영
        byte[] xlsx = mvc.perform(get("/api/reviews/{id}/corrective-action-ledger", reviewId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            Sheet action = wb.getSheet("시정조치서");
            assertThat(cell(action, 0, 4)).isEqualTo("1");  // 대상
            assertThat(cell(action, 0, 7)).isEqualTo("1");  // 완료 (실집계!)
            assertThat(cell(action, 0, 11)).isEqualTo("0"); // 잔여
            assertThat(cell(action, 3, 11)).isEqualTo("김PM");        // 조치담당자
            assertThat(cell(action, 3, 15)).isEqualTo("컬럼 추가 완료"); // 조치확인
            assertThat(cell(action, 3, 16)).isNotEmpty();               // 조치확인일
        }
    }

    private String cell(Sheet sheet, int r, int c) {
        Row row = sheet.getRow(r);
        return row == null || row.getCell(c) == null ? "" : fmt.formatCellValue(row.getCell(c));
    }
}
