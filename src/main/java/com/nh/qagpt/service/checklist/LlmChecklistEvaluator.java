package com.nh.qagpt.service.checklist;

import com.nh.qagpt.domain.Defect;
import com.nh.qagpt.domain.enums.ArtifactType;
import com.nh.qagpt.service.parser.ParsedDocument;

import java.util.List;

/**
 * LLM 기반 판정 (spec §3, §4.6, S2). 체크리스트를 기준으로 산출물을 검토하고
 * 각 결함에 심각도(개선/권고)·결함유형(6종)·관점(산출물/프로세스)·근거위치를 태깅한다.
 * 규칙검증({@link ChecklistEngine})을 보완하며, API 키 미설정 시 빈 목록을 반환(규칙검증만 동작).
 */
public interface LlmChecklistEvaluator {

    List<Defect> evaluate(ParsedDocument document, ArtifactType type);
}
