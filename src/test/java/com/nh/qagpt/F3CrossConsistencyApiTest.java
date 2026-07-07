package com.nh.qagpt;

import com.nh.qagpt.domain.Document;
import com.nh.qagpt.domain.Project;
import com.nh.qagpt.domain.ReviewResult;
import com.nh.qagpt.domain.enums.ArtifactType;
import com.nh.qagpt.repository.DocumentRepository;
import com.nh.qagpt.repository.ProjectRepository;
import com.nh.qagpt.repository.ReviewResultRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** [S8/후속] 교차 산출물 정합성 — 배치Job목록 Job ID ⊆ 배치설계서 검증 E2E. */
@SpringBootTest
@AutoConfigureMockMvc
class F3CrossConsistencyApiTest {

    @Autowired MockMvc mvc;
    @Autowired ProjectRepository projectRepository;
    @Autowired DocumentRepository documentRepository;
    @Autowired ReviewResultRepository reviewResultRepository;

    private void saveReview(Long projectId, ArtifactType type, String summaryJson) {
        Project p = projectRepository.findById(projectId).orElseThrow();
        Document doc = new Document();
        doc.setProject(p);
        doc.setArtifactType(type);
        doc.setStage(type.getStage());
        doc.setFileName(type.name() + ".xlsx");
        Document savedDoc = documentRepository.save(doc);

        ReviewResult r = new ReviewResult();
        r.setProject(p);
        r.setDocument(savedDoc);
        r.setRound(1);
        r.setRawResultJson(summaryJson);
        reviewResultRepository.save(r);
    }

    @Test
    void 배치Job목록ID가_배치설계서에_없으면_검출() throws Exception {
        Project p = new Project();
        p.setName("테스트");
        p.setCode("NHEFS");
        Long projectId = projectRepository.save(p).getId();

        saveReview(projectId, ArtifactType.BATCH_JOB_LIST,
                "{\"bodyRowCount\":3,\"ids\":[\"BJ-1\",\"BJ-2\",\"BJ-3\"]}");
        saveReview(projectId, ArtifactType.BATCH_DESIGN,
                "{\"bodyRowCount\":2,\"ids\":[\"BJ-1\",\"BJ-2\"]}");

        mvc.perform(get("/api/projects/{id}/cross-consistency", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defectCount").value(1))
                .andExpect(jsonPath("$.defects[0].description", containsString("BJ-3")))
                .andExpect(jsonPath("$.defects[0].description", containsString("배치설계서에 존재하지 않")));
    }

    @Test
    void 모든ID가_설계서에_있으면_결함없음() throws Exception {
        Project p = new Project();
        p.setName("테스트2");
        p.setCode("NHEFS");
        Long projectId = projectRepository.save(p).getId();

        saveReview(projectId, ArtifactType.BATCH_JOB_LIST,
                "{\"bodyRowCount\":2,\"ids\":[\"BJ-1\",\"BJ-2\"]}");
        saveReview(projectId, ArtifactType.BATCH_DESIGN,
                "{\"bodyRowCount\":3,\"ids\":[\"BJ-1\",\"BJ-2\",\"BJ-3\"]}");

        mvc.perform(get("/api/projects/{id}/cross-consistency", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defectCount").value(0));
    }
}
