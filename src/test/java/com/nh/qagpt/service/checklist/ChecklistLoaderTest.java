package com.nh.qagpt.service.checklist;

import com.nh.qagpt.domain.ChecklistItem;
import com.nh.qagpt.domain.enums.ArtifactType;
import com.nh.qagpt.domain.enums.Severity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChecklistLoaderTest {

    private final ChecklistLoader loader = new ChecklistLoader();

    @Test
    void 개선_권고_항목과_카테고리_힌트를_파싱한다() {
        String md = """
                # 배치Job목록 체크리스트

                ## 1. 파일명 검증

                ### [개선] 파일명 형식
                - 규칙: NH[프로젝트코드]-AN07-배치Job목록...
                - 문서번호와 파일명 일치

                ### [권고] 권장 표기
                - 권장 사항

                ## 체크리스트 요약

                | 구분 | 심각도 |
                |------|--------|
                | 파일명 | 개선 |
                """;

        List<ChecklistItem> items = loader.parse(md, "checklist_batch_job_list");

        // 요약 표는 항목이 아니므로 검사항목은 2건만
        assertThat(items).hasSize(2);

        ChecklistItem first = items.get(0);
        assertThat(first.getArtifactType()).isEqualTo(ArtifactType.BATCH_JOB_LIST);
        assertThat(first.getCategory()).isEqualTo("파일명 검증");
        assertThat(first.getSeverity()).isEqualTo(Severity.IMPROVEMENT);
        assertThat(first.getDescription()).isEqualTo("파일명 형식");
        assertThat(first.getRuleHint()).contains("규칙:").contains("문서번호와 파일명 일치");
        assertThat(first.getItemKey()).isEqualTo("checklist_batch_job_list-01");

        ChecklistItem second = items.get(1);
        assertThat(second.getSeverity()).isEqualTo(Severity.RECOMMENDATION);
        assertThat(second.getDescription()).isEqualTo("권장 표기");
        assertThat(second.getItemKey()).isEqualTo("checklist_batch_job_list-02");
    }

    @Test
    void 유형매핑_없는_체크리스트는_artifactType이_null이다() {
        String md = """
                # 산출물 세트 정합성 체크리스트

                ## 1. 비교 로직 공통 원칙

                ### [개선] 목록 ⊆ 실물
                - 누락 ID는 미제출로 보고
                """;

        List<ChecklistItem> items = loader.parse(md, "checklist_artifact_set_consistency");

        assertThat(items).hasSize(1);
        assertThat(items.get(0).getArtifactType()).isNull();
        assertThat(items.get(0).getCategory()).isEqualTo("비교 로직 공통 원칙");
    }
}
