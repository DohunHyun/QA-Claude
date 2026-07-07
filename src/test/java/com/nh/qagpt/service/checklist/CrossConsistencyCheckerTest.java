package com.nh.qagpt.service.checklist;

import com.nh.qagpt.domain.Defect;
import com.nh.qagpt.domain.enums.Severity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** [S8] 교차 산출물 정합성 + 요구사항 양방향 매핑. */
class CrossConsistencyCheckerTest {

    private final CrossConsistencyChecker checker = new CrossConsistencyChecker();

    @Test
    void 건수_일치하면_결함없음() {
        assertThat(checker.rowCountMismatch("프로그램목록", 10, "화면목록", 10)).isEmpty();
    }

    @Test
    void 건수_불일치_개선검출() {
        List<Defect> defects = checker.rowCountMismatch("프로그램목록", 10, "화면목록", 8);
        assertThat(defects).hasSize(1);
        assertThat(defects.get(0).getSeverity()).isEqualTo(Severity.IMPROVEMENT);
        assertThat(defects.get(0).getDescription()).contains("10건").contains("8건");
    }

    @Test
    void 양방향매핑_완전하면_결함없음() {
        List<Defect> defects = checker.bidirectionalMapping(
                Set.of("REQ-1", "REQ-2"), Set.of("REQ-1", "REQ-2"), "요구사항", "설계");
        assertThat(defects).isEmpty();
    }

    @Test
    void 요구사항_대응누락_검출() {
        List<Defect> defects = checker.bidirectionalMapping(
                Set.of("REQ-1", "REQ-2"), Set.of("REQ-1"), "요구사항", "설계");
        assertThat(defects).hasSize(1);
        assertThat(defects.get(0).getLocationId()).isEqualTo("REQ-2");
        assertThat(defects.get(0).getDescription()).contains("대응하는 설계 매핑이 없");
    }

    @Test
    void 설계_역추적_근거요구사항_누락_검출() {
        List<Defect> defects = checker.bidirectionalMapping(
                Set.of("REQ-1"), Set.of("REQ-1", "REQ-9"), "요구사항", "설계");
        assertThat(defects).anySatisfy(d -> {
            assertThat(d.getLocationId()).isEqualTo("REQ-9");
            assertThat(d.getDescription()).contains("근거 요구사항");
        });
    }
}
