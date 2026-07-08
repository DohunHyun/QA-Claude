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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** [P1-F/G] 추적표↔UI목록 교차 검증 + 프로젝트 승인 플로우 E2E. */
@SpringBootTest
@AutoConfigureMockMvc
class TraceabilityUiCrossApiTest {

    @Autowired MockMvc mvc;
    @Autowired ProjectRepository projectRepository;
    @Autowired DocumentRepository documentRepository;
    @Autowired ReviewResultRepository reviewResultRepository;

    private Long project() {
        Project p = new Project();
        p.setName("교차테스트");
        p.setCode("NHEFS");
        return projectRepository.save(p).getId();
    }

    private void seed(Long projectId, ArtifactType type, String summaryJson) {
        Project p = projectRepository.findById(projectId).orElseThrow();
        Document doc = new Document();
        doc.setProject(p);
        doc.setArtifactType(type);
        doc.setStage(type.getStage());
        Document saved = documentRepository.save(doc);
        ReviewResult r = new ReviewResult();
        r.setProject(p);
        r.setDocument(saved);
        r.setRound(1);
        r.setRawResultJson(summaryJson);
        reviewResultRepository.save(r);
    }

    @Test
    void 추적표UID가_UI목록에_없으면_검출() throws Exception {
        Long projectId = project();
        seed(projectId, ArtifactType.REQUIREMENT_TRACEABILITY_MATRIX,
                "{\"bodyRowCount\":2,\"ids\":[\"EFDSBAQ0I0\",\"EFDSBAQ0I9\"]}");
        seed(projectId, ArtifactType.UI_LIST,
                "{\"bodyRowCount\":1,\"ids\":[\"EFDSBAQ0I0\"]}");

        mvc.perform(get("/api/projects/{id}/cross-consistency", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defectCount").value(1))
                .andExpect(jsonPath("$.defects[0].description", containsString("EFDSBAQ0I9")))
                .andExpect(jsonPath("$.defects[0].description", containsString("UI목록에 존재하지 않")));
    }

    @Test
    void 프로젝트_승인시_ACTIVE_전이() throws Exception {
        Long projectId = project();
        mvc.perform(post("/api/projects/{id}/approve", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}
