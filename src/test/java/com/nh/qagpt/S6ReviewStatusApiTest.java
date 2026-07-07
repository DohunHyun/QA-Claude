package com.nh.qagpt;

import com.nh.qagpt.domain.Project;
import com.nh.qagpt.repository.ProjectRepository;
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
import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** [S6] 재검증 회차 증가 + 회차별 현황(대상/통과회차) + 단계 게이트 E2E. */
@SpringBootTest
@AutoConfigureMockMvc
class S6ReviewStatusApiTest {

    private static final String XLSX_MIME =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Autowired
    MockMvc mvc;

    @Autowired
    ProjectRepository projectRepository;

    private Long registerProject() {
        Project p = new Project();
        p.setName("테스트검증프로젝트");
        p.setCode("NHEFS");
        p.setAnalysisStart(LocalDate.of(2025, 1, 1));
        p.setAnalysisEnd(LocalDate.of(2025, 12, 31));
        return projectRepository.save(p).getId();
    }

    /** 표지/개정이력 정상 + 본문 헤더 가변 (Phase1 무결함, 본문만 통과여부 제어). */
    private byte[] batchWorkbook(List<String> bodyHeader) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet cover = wb.createSheet("표지");
            addRow(cover, 0, "문서번호", "NHEFS-EA-AN07");
            addRow(cover, 1, "프로젝트명", "테스트검증프로젝트");
            addRow(cover, 2, "제·개정일자", "2025-06-01");

            Sheet rev = wb.createSheet("개정이력");
            addRow(rev, 0, "버전", "변경일", "작성자");
            addRow(rev, 1, "1.0", "2025-06-01", "홍길동");

            Sheet body = wb.createSheet("본문");
            Row h = body.createRow(0);
            for (int i = 0; i < bodyHeader.size(); i++) {
                h.createCell(i).setCellValue(bodyHeader.get(i));
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    private void addRow(Sheet sheet, int idx, String... cells) {
        Row row = sheet.createRow(idx);
        for (int i = 0; i < cells.length; i++) {
            row.createCell(i).setCellValue(cells[i]);
        }
    }

    private void validate(Long projectId, byte[] xlsx) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file",
                "NHEFS-EA-AN07-배치Job목록_V1.0_20250601.xlsx", XLSX_MIME, xlsx);
        mvc.perform(multipart("/api/validate").file(file).param("projectId", String.valueOf(projectId)))
                .andExpect(status().isOk());
    }

    @Test
    void 재검증_회차증가_통과회차_단계게이트() throws Exception {
        Long projectId = registerProject();

        // 1회차: 본문 단위업무명 누락 → 개선 1건 → 통과 불가
        validate(projectId, batchWorkbook(List.of("배치Job ID")));
        // 2회차(재검증): 필수 컬럼 보완 → 통과
        validate(projectId, batchWorkbook(List.of("단위업무명", "배치Job ID")));

        mvc.perform(get("/api/projects/{id}/review-status", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.artifacts[0].artifactType").value("BATCH_JOB_LIST"))
                .andExpect(jsonPath("$.artifacts[0].passed").value(true))
                .andExpect(jsonPath("$.artifacts[0].passedAtRound").value(2))
                .andExpect(jsonPath("$.artifacts[0].rounds.length()").value(2))
                .andExpect(jsonPath("$.artifacts[0].rounds[0].round").value(1))
                .andExpect(jsonPath("$.artifacts[0].rounds[0].passed").value(false))
                .andExpect(jsonPath("$.artifacts[0].rounds[0].improvementCount").value(1))
                .andExpect(jsonPath("$.artifacts[0].rounds[0].target").value(1))
                .andExpect(jsonPath("$.artifacts[0].rounds[0].remaining").value(1))
                .andExpect(jsonPath("$.artifacts[0].rounds[1].round").value(2))
                .andExpect(jsonPath("$.artifacts[0].rounds[1].passed").value(true))
                .andExpect(jsonPath("$.artifacts[0].rounds[1].target").value(0))
                // 분석 단계 통과 → 다음 단계(설계) 진행 가능
                .andExpect(jsonPath("$.stages[?(@.stage=='ANALYSIS')].passed").value(true))
                .andExpect(jsonPath("$.stages[?(@.stage=='ANALYSIS')].nextStage").value("설계"))
                .andExpect(jsonPath("$.stages[?(@.stage=='ANALYSIS')].nextStageUnlocked").value(true));
    }
}
