package com.nh.qagpt;

import com.nh.qagpt.domain.Defect;
import com.nh.qagpt.domain.ReviewResult;
import com.nh.qagpt.domain.enums.DefectType;
import com.nh.qagpt.domain.enums.Perspective;
import com.nh.qagpt.domain.enums.Severity;
import com.nh.qagpt.domain.enums.Stage;
import com.nh.qagpt.repository.ReviewResultRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** [S7] QA 승인 절차 + 예외 승인 경로 + 승인 후 검토결과서 발급 E2E. */
@SpringBootTest
@AutoConfigureMockMvc
class S7QaApprovalApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ReviewResultRepository reviewResultRepository;

    private Long savePassed(boolean passed) {
        ReviewResult r = new ReviewResult();
        r.setStage(Stage.ANALYSIS);
        r.setPassed(passed);
        if (!passed) {
            Defect d = new Defect();
            d.setSeverity(Severity.IMPROVEMENT);
            d.setDefectType(DefectType.MISSING_REQUIRED);
            d.setPerspective(Perspective.ARTIFACT);
            d.setDescription("필수 컬럼 누락");
            r.addDefect(d);
        }
        return reviewResultRepository.save(r).getId();
    }

    @Test
    void 승인전_발급시도_409() throws Exception {
        Long id = savePassed(true);
        mvc.perform(get("/api/reviews/{id}/review-report", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("QA 승인 후")));
    }

    @Test
    void 개선잔존_예외없이_승인시도_409() throws Exception {
        Long id = savePassed(false);
        mvc.perform(post("/api/reviews/{id}/approve", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("예외 승인")));
    }

    @Test
    void 개선잔존_예외승인_후_발급성공() throws Exception {
        Long id = savePassed(false);
        mvc.perform(post("/api/reviews/{id}/approve", id).param("exception", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qaApproved").value(true))
                .andExpect(jsonPath("$.qaException").value(true));

        mvc.perform(get("/api/reviews/{id}/review-report", id))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("application/hwp+zip")))
                .andExpect(header().string("Content-Disposition", containsString(".hwpx")));
    }

    @Test
    void 통과결과_정상승인_후_발급성공() throws Exception {
        Long id = savePassed(true);
        mvc.perform(post("/api/reviews/{id}/approve", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qaApproved").value(true))
                .andExpect(jsonPath("$.qaException").value(false));

        mvc.perform(get("/api/reviews/{id}/review-report", id))
                .andExpect(status().isOk());
    }
}
