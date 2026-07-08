package com.nh.qagpt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [P0-B/D] 파일 저장 → 비동기 검증(202→폴링→COMPLETED) + 다중 업로드 + 저장원본 개선산출물 E2E.
 * spec §10.1(비동기)·§4.1(다중 업로드)·§4.3(원본 포맷 유지) 검증.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AsyncPipelineApiTest {

    private static final String XLSX_MIME =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    private byte[] batchXlsx(boolean withRequiredCols) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("본문");
            Row h = sheet.createRow(0);
            if (withRequiredCols) {
                h.createCell(0).setCellValue("단위업무명");
                h.createCell(1).setCellValue("배치Job ID");
            } else {
                h.createCell(0).setCellValue("배치Job ID");
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    private Long createProject() throws Exception {
        MvcResult res = mvc.perform(post("/api/projects")
                        .contentType("application/json")
                        .content("{\"name\":\"비동기테스트\",\"code\":\"NHEFS\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return mapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void 다중업로드_비동기검증_폴링으로_완료확인() throws Exception {
        Long projectId = createProject();

        // 1) 다중 업로드 (한 요청에 2개) — 파일 저장 + 문서 2건 생성
        MvcResult uploadRes = mvc.perform(multipart("/api/documents")
                        .file(new MockMultipartFile("file", "NHEFS-EA-AN07-배치Job목록_V1.0_20250601.xlsx",
                                XLSX_MIME, batchXlsx(true)))
                        .file(new MockMultipartFile("file", "NHEFS-EA-DS07-프로그램목록_V1.0_20250601.xlsx",
                                XLSX_MIME, batchXlsx(true)))
                        .param("projectId", String.valueOf(projectId)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode docs = mapper.readTree(uploadRes.getResponse().getContentAsString());
        assertThat(docs).hasSize(2);
        long documentId = docs.get(0).get("documentId").asLong();

        // 2) 비동기 검증 트리거 → 202
        mvc.perform(post("/api/reviews").param("documentId", String.valueOf(documentId)))
                .andExpect(status().isAccepted());

        // 3) 폴링 — COMPLETED까지 (최대 10초)
        JsonNode review = pollUntilDone(documentId);
        assertThat(review.get("status").asText()).isEqualTo("COMPLETED");
        assertThat(review.get("passed").asBoolean()).isTrue(); // 필수컬럼 갖춘 정상 문서
    }

    @Test
    void validate_batch_혼합다중파일_각각_자동인식() throws Exception {
        Long projectId = createProject();
        MvcResult res = mvc.perform(multipart("/api/validate-batch")
                        .file(new MockMultipartFile("files", "NHEFS-EA-AN07-배치Job목록_V1.0_20250601.xlsx",
                                XLSX_MIME, batchXlsx(true)))
                        .file(new MockMultipartFile("files", "NHEFS-EA-DS07-프로그램목록_V1.0_20250601.xlsx",
                                XLSX_MIME, batchXlsx(true)))
                        .param("projectId", String.valueOf(projectId)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode results = mapper.readTree(res.getResponse().getContentAsString());
        assertThat(results).hasSize(2);
        assertThat(results.get(0).get("artifactType").asText()).isEqualTo("BATCH_JOB_LIST");
        assertThat(results.get(1).get("artifactType").asText()).isEqualTo("PROGRAM_LIST");
    }

    @Test
    void 저장원본으로_개선산출물_GET_재업로드없이() throws Exception {
        // 결함 있는 배치Job목록 검증(원본 저장됨)
        MvcResult res = mvc.perform(multipart("/api/validate")
                        .file(new MockMultipartFile("file", "NHEFS-EA-AN07-배치Job목록_V1.0_20250601.xlsx",
                                XLSX_MIME, batchXlsx(false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passed").value(false))
                .andReturn();
        long reviewId = mapper.readTree(res.getResponse().getContentAsString()).get("reviewId").asLong();

        mvc.perform(get("/api/reviews/{id}/improved-artifact", reviewId))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentType())
                        .contains("spreadsheetml"));
    }

    private JsonNode pollUntilDone(long documentId) throws Exception {
        for (int i = 0; i < 50; i++) {
            MvcResult res = mvc.perform(get("/api/reviews/by-document/{id}", documentId))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode list = mapper.readTree(res.getResponse().getContentAsString());
            if (!list.isEmpty()) {
                String status = list.get(list.size() - 1).get("status").asText();
                if (List.of("COMPLETED", "FAILED").contains(status)) {
                    return list.get(list.size() - 1);
                }
            }
            Thread.sleep(200);
        }
        throw new AssertionError("비동기 검증이 10초 내 완료되지 않음");
    }
}
