package com.nh.qagpt.service.checklist;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nh.qagpt.config.ClaudeProperties;
import com.nh.qagpt.domain.ChecklistItem;
import com.nh.qagpt.domain.Defect;
import com.nh.qagpt.domain.enums.ArtifactType;
import com.nh.qagpt.domain.enums.DefectType;
import com.nh.qagpt.domain.enums.Perspective;
import com.nh.qagpt.domain.enums.Severity;
import com.nh.qagpt.repository.ChecklistItemRepository;
import com.nh.qagpt.service.ai.ClaudeClient;
import com.nh.qagpt.service.parser.ParsedDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@link LlmChecklistEvaluator} 구현. DB에 적재된 체크리스트 항목을 기준으로 Claude에 판정을 요청하고,
 * 응답 JSON을 {@link Defect}로 매핑한다. 재현성을 위해 온도 0.0으로 호출한다(spec S2 수락기준).
 *
 * AI 동작 원칙(spec §4.6): 빈 항목은 추측하지 않고 결함으로만 보고, 근거 없는 주관 판단 금지,
 * 생성 텍스트에 한자 사용 금지, 근거위치(시트/행/열/ID) 필수.
 */
@Service
public class LlmChecklistEvaluatorImpl implements LlmChecklistEvaluator {

    private static final Logger log = LoggerFactory.getLogger(LlmChecklistEvaluatorImpl.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 본문 시트에서 프롬프트에 포함할 최대 행 수(토큰 절약). */
    private static final int MAX_ROWS_PER_SHEET = 60;

    private final ClaudeClient claude;
    private final ClaudeProperties props;
    private final ChecklistItemRepository checklistItemRepository;

    public LlmChecklistEvaluatorImpl(ClaudeClient claude,
                                     ClaudeProperties props,
                                     ChecklistItemRepository checklistItemRepository) {
        this.claude = claude;
        this.props = props;
        this.checklistItemRepository = checklistItemRepository;
    }

    @Override
    public List<Defect> evaluate(ParsedDocument document, ArtifactType type) {
        if (type == null || type == ArtifactType.UNKNOWN) {
            return List.of();
        }
        if (!props.hasApiKey()) {
            log.info("CLAUDE_API_KEY 미설정 — LLM 판정 생략, 규칙검증만 수행 ({})", type.getLabel());
            return List.of();
        }

        List<ChecklistItem> items = checklistItemRepository.findByArtifactType(type);
        if (items.isEmpty()) {
            log.info("체크리스트 항목 없음 — LLM 판정 생략 ({})", type.getLabel());
            return List.of();
        }

        try {
            String response = claude.complete(systemPrompt(), userPrompt(document, type, items), 0.0);
            return parseDefects(response);
        } catch (RuntimeException e) {
            // LLM 실패가 전체 검증을 막지 않도록 규칙검증 결과만 유지 (조용한 성공 아님 — 경고 로그)
            log.warn("LLM 판정 실패({}): {}", type.getLabel(), e.getMessage());
            return List.of();
        }
    }

    private String systemPrompt() {
        return """
                너는 IT SI 산출물 품질검토 QA 봇이다. 주어진 체크리스트를 기준으로만 산출물을 판정한다.
                동작 원칙:
                - 심각도는 개선(필수 수정) 또는 권고(권장) 둘 중 하나로 판정한다.
                - 근거 없는 주관 판단 금지. 체크리스트에 근거가 있는 결함만 보고한다.
                - 빈 항목/누락은 추측해서 채우지 말고 결함으로 보고한다.
                - 모든 결함에 근거위치(시트명/행/열/ID 중 가능한 것)를 명시한다.
                - 생성 텍스트에 한자 사용 금지(기술 영문 약어는 허용).
                출력은 반드시 아래 스키마의 JSON 배열만 반환한다. 설명/마크다운/코드펜스 금지.
                [
                  {
                    "severity": "개선|권고",
                    "defectType": "표준미준수|필수항목누락|내용오류·불명확|미제출|중복|기타",
                    "perspective": "산출물|프로세스",
                    "locationSheet": "", "locationRow": "", "locationColumn": "", "locationId": "",
                    "description": "결함 내용",
                    "improvementGuide": "개선 방향",
                    "checklistItemKey": "근거 체크리스트 itemKey"
                  }
                ]
                결함이 없으면 빈 배열 []을 반환한다.
                """;
    }

    private String userPrompt(ParsedDocument document, ArtifactType type, List<ChecklistItem> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("산출물 유형: ").append(type.getLabel()).append('\n');
        sb.append("파일명: ").append(document.getFileName()).append("\n\n");

        sb.append("[체크리스트]\n");
        for (ChecklistItem it : items) {
            sb.append("- key=").append(it.getItemKey())
              .append(" | 심각도=").append(it.getSeverity() == null ? "" : it.getSeverity().getLabel())
              .append(" | 구분=").append(it.getCategory() == null ? "" : it.getCategory())
              .append(" | 검사=").append(it.getDescription());
            if (it.getRuleHint() != null && !it.getRuleHint().isBlank()) {
                sb.append(" | 규칙=").append(it.getRuleHint());
            }
            sb.append('\n');
        }

        sb.append("\n[산출물 내용]\n");
        if (document.getSheets().isEmpty() && document.getRawText() != null) {
            sb.append(document.getRawText());
        } else {
            for (Map.Entry<String, List<List<String>>> sheet : document.getSheets().entrySet()) {
                sb.append("## 시트: ").append(sheet.getKey()).append('\n');
                List<List<String>> rows = sheet.getValue();
                int limit = Math.min(rows.size(), MAX_ROWS_PER_SHEET);
                for (int i = 0; i < limit; i++) {
                    sb.append("r").append(i).append(": ").append(rows.get(i)).append('\n');
                }
                if (rows.size() > limit) {
                    sb.append("...(이하 ").append(rows.size() - limit).append("행 생략)\n");
                }
            }
        }
        return sb.toString();
    }

    /** 응답 JSON(배열)을 Defect 리스트로 매핑. 배열 이외의 텍스트가 섞여 와도 첫 배열만 추출. */
    private List<Defect> parseDefects(String response) {
        List<Defect> defects = new ArrayList<>();
        if (response == null || response.isBlank()) {
            return defects;
        }
        String json = extractJsonArray(response);
        if (json == null) {
            log.warn("LLM 응답에서 JSON 배열을 찾지 못함: {}", truncate(response));
            return defects;
        }
        try {
            JsonNode arr = MAPPER.readTree(json);
            if (!arr.isArray()) {
                return defects;
            }
            for (JsonNode node : arr) {
                Defect d = new Defect();
                d.setSeverity(parseSeverity(text(node, "severity")));
                d.setDefectType(parseDefectType(text(node, "defectType")));
                d.setPerspective(parsePerspective(text(node, "perspective")));
                d.setLocationSheet(blankToNull(text(node, "locationSheet")));
                d.setLocationRow(blankToNull(text(node, "locationRow")));
                d.setLocationColumn(blankToNull(text(node, "locationColumn")));
                d.setLocationId(blankToNull(text(node, "locationId")));
                d.setDescription(text(node, "description"));
                d.setImprovementGuide(blankToNull(text(node, "improvementGuide")));
                d.setChecklistItemKey(blankToNull(text(node, "checklistItemKey")));
                defects.add(d);
            }
        } catch (Exception e) {
            log.warn("LLM 응답 JSON 파싱 실패: {} — {}", truncate(response), e.getMessage());
        }
        return defects;
    }

    private static String extractJsonArray(String s) {
        int start = s.indexOf('[');
        int end = s.lastIndexOf(']');
        if (start < 0 || end <= start) {
            return null;
        }
        return s.substring(start, end + 1);
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private static Severity parseSeverity(String label) {
        if (label == null) {
            return Severity.IMPROVEMENT;
        }
        for (Severity s : Severity.values()) {
            if (s.getLabel().equals(label.trim())) {
                return s;
            }
        }
        return Severity.IMPROVEMENT;
    }

    private static DefectType parseDefectType(String label) {
        if (label != null) {
            for (DefectType t : DefectType.values()) {
                if (t.getLabel().equals(label.trim())) {
                    return t;
                }
            }
        }
        return DefectType.ETC;
    }

    private static Perspective parsePerspective(String label) {
        if (label != null) {
            for (Perspective p : Perspective.values()) {
                if (p.getLabel().equals(label.trim())) {
                    return p;
                }
            }
        }
        return Perspective.ARTIFACT;
    }

    private static String truncate(String s) {
        return s.length() <= 300 ? s : s.substring(0, 300) + "…";
    }
}
