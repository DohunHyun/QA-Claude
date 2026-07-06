package com.nh.qagpt;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** [S1] 업로드→검증→결과 HTTP 전 구간 E2E (MockMvc). */
@SpringBootTest
@AutoConfigureMockMvc
class S1ValidationApiTest {

    private static final String XLSX_MIME =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Autowired
    MockMvc mvc;

    @Test
    void 정상_배치Job목록_업로드시_결함없음() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "batch.xlsx", XLSX_MIME,
                workbook(List.of("단위업무명", "배치Job ID", "업무명"), List.of("여신", "BJ-DE-0001", "일일정산배치")));

        mvc.perform(multipart("/api/validate").file(file).param("artifactType", "BATCH_JOB_LIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passed").value(true))
                .andExpect(jsonPath("$.defectCount").value(0))
                .andExpect(jsonPath("$.message").value("결함 없음"));
    }

    @Test
    void 단위업무명컬럼_누락_업로드시_개선결함1건() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "batch.xlsx", XLSX_MIME,
                workbook(List.of("배치Job ID", "업무명"), List.of("BJ-DE-0001", "일일정산배치")));

        mvc.perform(multipart("/api/validate").file(file).param("artifactType", "BATCH_JOB_LIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passed").value(false))
                .andExpect(jsonPath("$.defectCount").value(1))
                .andExpect(jsonPath("$.defects[0].severity").value("개선"))
                .andExpect(jsonPath("$.defects[0].defectType").value("필수항목누락"))
                .andExpect(jsonPath("$.defects[0].location", containsString("단위업무명")));
    }

    @Test
    void 파싱불가_파일_업로드시_400() throws Exception {
        MockMultipartFile broken = new MockMultipartFile("file", "broken.xlsx", XLSX_MIME,
                "이건 xlsx가 아님".getBytes());

        mvc.perform(multipart("/api/validate").file(broken).param("artifactType", "BATCH_JOB_LIST"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("파싱 실패")));
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
