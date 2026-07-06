package com.nh.qagpt.service.classifier;

import com.nh.qagpt.config.ClaudeProperties;
import com.nh.qagpt.domain.enums.ArtifactType;
import com.nh.qagpt.service.ai.ClaudeClient;
import com.nh.qagpt.service.parser.ParsedDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 산출물 유형 자동 인식 (spec §4.2, S2 수락기준).
 *
 * 하이브리드 전략:
 *  1) 파일명의 단위업무/문서 코드(AN07·DS09·PM-120 …)로 결정적 매핑 — 재현 가능(같은 입력→같은 유형).
 *  2) 코드가 없으면 파일명에 포함된 산출물 한글명(라벨)으로 매핑.
 *  3) 그래도 실패하고 API 키가 있으면 Claude에 시트 구조/파일명을 근거로 질의(폴백).
 *  4) 모두 실패 시 {@link ArtifactType#UNKNOWN}.
 *
 * NH 표준 산출물은 파일명이 규격화(예 NHEFS-EA-AN07-배치Job목록)돼 있어 1)만으로 대부분 판별된다.
 */
@Service
public class ClaudeDocumentClassifier implements DocumentClassifier {

    private static final Logger log = LoggerFactory.getLogger(ClaudeDocumentClassifier.class);

    /** 파일명 코드 → 산출물 유형. 순서 보존(더 구체적인 PM-NNN을 먼저 검사). */
    private static final Map<String, ArtifactType> CODE_MAP = new LinkedHashMap<>();
    static {
        CODE_MAP.put("PM-141", ArtifactType.DOCUMENT_WRITING_GUIDELINE);
        CODE_MAP.put("PM-120", ArtifactType.TAILORING_RESULT);
        CODE_MAP.put("PM-310", ArtifactType.REQUIREMENT_TRACEABILITY_MATRIX);
        CODE_MAP.put("AN04", ArtifactType.PROCESS_DEFINITION);
        CODE_MAP.put("AN06", ArtifactType.REQUIREMENT_DEFINITION);
        CODE_MAP.put("AN07", ArtifactType.BATCH_JOB_LIST);
        CODE_MAP.put("AN08", ArtifactType.INTERFACE_DEFINITION);
        CODE_MAP.put("DS01", ArtifactType.UI_LIST);
        CODE_MAP.put("DS07", ArtifactType.PROGRAM_LIST);
        CODE_MAP.put("DS09", ArtifactType.BATCH_DESIGN);
        CODE_MAP.put("DS10", ArtifactType.INTERFACE_DESIGN);
    }

    private final ClaudeClient claude;
    private final ClaudeProperties props;

    public ClaudeDocumentClassifier(ClaudeClient claude, ClaudeProperties props) {
        this.claude = claude;
        this.props = props;
    }

    @Override
    public ArtifactType classify(ParsedDocument document) {
        String fileName = document.getFileName() == null ? "" : document.getFileName();

        ArtifactType byCode = byFileCode(fileName);
        if (byCode != null) {
            return byCode;
        }
        ArtifactType byLabel = byFileLabel(fileName);
        if (byLabel != null) {
            return byLabel;
        }
        if (props.hasApiKey()) {
            ArtifactType byLlm = byClaude(document);
            if (byLlm != null) {
                return byLlm;
            }
        }
        return ArtifactType.UNKNOWN;
    }

    /** 파일명에 포함된 표준 코드(하이픈 유무 무시)로 매핑. */
    private ArtifactType byFileCode(String fileName) {
        String upper = fileName.toUpperCase();
        String noHyphen = upper.replace("-", "");
        for (Map.Entry<String, ArtifactType> e : CODE_MAP.entrySet()) {
            String code = e.getKey();
            if (upper.contains(code) || noHyphen.contains(code.replace("-", ""))) {
                return e.getValue();
            }
        }
        return null;
    }

    /** 파일명에 산출물 한글명이 그대로 들어있으면 매핑(공백 제거 비교). */
    private ArtifactType byFileLabel(String fileName) {
        String norm = fileName.replaceAll("\\s+", "");
        for (ArtifactType type : ArtifactType.values()) {
            if (type == ArtifactType.UNKNOWN || type.getLabel() == null) {
                continue;
            }
            if (norm.contains(type.getLabel().replaceAll("\\s+", ""))) {
                return type;
            }
        }
        return null;
    }

    /** Claude 폴백: 파일명·시트명을 근거로 유형 코드를 되묻는다. 실패 시 null(호출측이 UNKNOWN 처리). */
    private ArtifactType byClaude(ParsedDocument document) {
        try {
            StringBuilder candidates = new StringBuilder();
            for (ArtifactType t : ArtifactType.values()) {
                if (t != ArtifactType.UNKNOWN) {
                    candidates.append("- ").append(t.name()).append(" : ").append(t.getLabel()).append('\n');
                }
            }
            String system = "너는 IT SI 산출물 유형 분류기다. 아래 후보 중 정확히 하나의 enum 이름만 대문자로 답하라. "
                    + "확신이 없으면 UNKNOWN. 다른 말/설명/한자 금지.";
            String user = "후보:\n" + candidates
                    + "\n파일명: " + document.getFileName()
                    + "\n시트: " + document.getSheets().keySet();
            String answer = claude.complete(system, user, 0.0);
            if (answer == null) {
                return null;
            }
            String token = answer.trim().toUpperCase().replaceAll("[^A-Z_]", "");
            for (ArtifactType t : ArtifactType.values()) {
                if (t.name().equals(token)) {
                    return t == ArtifactType.UNKNOWN ? null : t;
                }
            }
        } catch (RuntimeException e) {
            log.warn("Claude 유형 분류 폴백 실패({}): {}", document.getFileName(), e.getMessage());
        }
        return null;
    }
}
