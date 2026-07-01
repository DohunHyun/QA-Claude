package com.nh.qagpt.service.checklist;

import com.nh.qagpt.domain.Defect;
import com.nh.qagpt.domain.Project;
import com.nh.qagpt.domain.enums.ArtifactType;
import com.nh.qagpt.service.ai.ClaudeClient;
import com.nh.qagpt.service.parser.ParsedDocument;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 룰 엔진 + LLM 하이브리드 검증 (spec §5, §7.2).
 * 4-Phase 고정 순서: ① 문서작성 표준 보증(파일명→표지→개정이력) → ② 요구사항 품질 보증(추적표 기반 누락)
 *                  → ③ 목록/정합성(필수컬럼·ID중복·enum·교차 정합성) → ④ 결과물 생성(호출측).
 * 명명규칙은 하드코딩하지 않고 1순위 프로젝트정보 · 2순위 문서작성지침서(PM-141-01) 파싱으로 보충한다.
 */
@Service
public class ChecklistEngineImpl implements ChecklistEngine {

    private final ClaudeClient claude;

    public ChecklistEngineImpl(ClaudeClient claude) {
        this.claude = claude;
    }

    @Override
    public List<Defect> apply(ParsedDocument document, ArtifactType type, Project project) {
        // TODO: 유형별 체크리스트 로드 → 룰 검증(필수컬럼/ID중복/명명규칙/enum) + LLM 판정 병합.
        //       각 결함에 심각도·결함유형·관점·근거위치를 태깅한다.
        //       빈 항목은 추측 금지, 결함으로만 보고 (AI 동작 원칙 ①②).
        throw new UnsupportedOperationException("TODO: 4-Phase 체크리스트 적용 엔진 구현");
    }
}
