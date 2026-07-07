package com.nh.qagpt.service.checklist;

import java.util.List;

/**
 * [S8/후속] 교차 산출물 정합성용 경량 요약. 검증 시 추출해 ReviewResult.rawResultJson에 저장한다.
 * 파일 원본을 보관하지 않고도 프로젝트 단위로 산출물 간 대조(건수·ID 정합성)를 할 수 있게 한다.
 */
public record ArtifactSummary(int bodyRowCount, List<String> ids) {

    public ArtifactSummary {
        ids = ids == null ? List.of() : List.copyOf(ids);
    }
}
