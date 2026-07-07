package com.nh.qagpt.service.checklist;

import com.nh.qagpt.domain.Defect;
import com.nh.qagpt.domain.enums.ArtifactType;
import com.nh.qagpt.domain.enums.Perspective;
import com.nh.qagpt.domain.enums.Severity;
import com.nh.qagpt.service.parser.ParsedDocument;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** [S8/후속] 요구사항추적표 양방향(요구사항→설계) 매핑 누락 검증. PM-310 실측 레이아웃 기준. */
class Phase2TraceabilityValidatorTest {

    private final Phase2TraceabilityValidator validator = new Phase2TraceabilityValidator();

    /** PM-310 AP&B2B 시트 축약: 헤더행에 요구사항 ID / 액티비티 ID / U ID. */
    private ParsedDocument matrix(List<List<String>> dataRows) {
        ParsedDocument d = new ParsedDocument();
        java.util.List<java.util.List<String>> rows = new java.util.ArrayList<>();
        rows.add(Arrays.asList("요구사항 ID", "요구사항 명", "액티비티 ID", "U ID"));
        rows.addAll(dataRows);
        d.getSheets().put("AP&B2B", rows);
        return d;
    }

    @Test
    void 요구사항에_설계매핑_있으면_결함없음() {
        List<Defect> defects = validator.validate(matrix(List.of(
                Arrays.asList("RQM-RF-EA-0001", "서류서비스", "AV-EA-DS-0001", "EFDSBAQ0I0"),
                Arrays.asList("RQM-RF-EA-0002", "서류내역", "AV-EA-DS-0011", ""))),
                ArtifactType.REQUIREMENT_TRACEABILITY_MATRIX);
        assertThat(defects).isEmpty();
    }

    @Test
    void 요구사항에_설계매핑_없으면_개선검출() {
        List<Defect> defects = validator.validate(matrix(List.of(
                Arrays.asList("RQM-RF-EA-0003", "매핑없는 요구사항", "", ""))),
                ArtifactType.REQUIREMENT_TRACEABILITY_MATRIX);
        assertThat(defects).hasSize(1);
        Defect d = defects.get(0);
        assertThat(d.getSeverity()).isEqualTo(Severity.IMPROVEMENT);
        assertThat(d.getPerspective()).isEqualTo(Perspective.PROCESS);
        assertThat(d.getLocationId()).isEqualTo("RQM-RF-EA-0003");
        assertThat(d.getDescription()).contains("대응하는 설계");
    }

    @Test
    void 요구사항ID_빈행은_검사제외() {
        List<Defect> defects = validator.validate(matrix(List.of(
                Arrays.asList("", "", "", ""))),
                ArtifactType.REQUIREMENT_TRACEABILITY_MATRIX);
        assertThat(defects).isEmpty();
    }

    @Test
    void 다른유형은_검사안함() {
        assertThat(validator.validate(matrix(List.of(
                Arrays.asList("RQM-1", "x", "", ""))), ArtifactType.BATCH_JOB_LIST)).isEmpty();
    }
}
