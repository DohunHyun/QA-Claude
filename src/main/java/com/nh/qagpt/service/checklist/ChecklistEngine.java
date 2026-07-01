package com.nh.qagpt.service.checklist;

import com.nh.qagpt.domain.Defect;
import com.nh.qagpt.domain.Project;
import com.nh.qagpt.domain.enums.ArtifactType;
import com.nh.qagpt.service.parser.ParsedDocument;

import java.util.List;

/**
 * 검증 정확도의 핵심 시임. 산출물 유형별 체크리스트(docs/checklists/)를 적용해 결함 목록을 산출한다.
 * 회귀 테스트는 PoC 시정조치서 36건(CA_P01~CA_W34)을 정답지로 이 엔진을 검증한다.
 */
public interface ChecklistEngine {

    List<Defect> apply(ParsedDocument document, ArtifactType type, Project project);
}
