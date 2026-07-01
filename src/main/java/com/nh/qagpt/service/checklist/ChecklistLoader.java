package com.nh.qagpt.service.checklist;

import com.nh.qagpt.domain.ChecklistItem;
import com.nh.qagpt.domain.enums.ArtifactType;
import com.nh.qagpt.domain.enums.Severity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * docs/checklists/*.md 를 {@link ChecklistItem} 목록으로 파싱한다.
 *
 * 파싱 규칙:
 * <ul>
 *   <li>{@code ## N. 섹션명}  → category (선두 "N." 제거)</li>
 *   <li>{@code ### [개선]/[권고] 항목명} → 검사항목 1건 (severity + description)</li>
 *   <li>항목 하위 {@code - 불릿} → ruleHint (여러 줄이면 개행으로 합침)</li>
 *   <li>판정어휘([개선]/[권고]) 없는 {@code ###}, 표·일반 문단(요약 표 등)은 무시</li>
 * </ul>
 *
 * phase·defectType·perspective 는 체크리스트에 항목별로 명시돼 있지 않으므로 비워둔다.
 * (개별 결함의 결함유형·관점 태깅은 판정 단계(S2)의 몫 — 시더는 "검사 정의"만 충실히 적재한다.)
 */
@Component
public class ChecklistLoader {

    public List<ChecklistItem> parse(String markdown, String fileBaseName) {
        ArtifactType artifactType = artifactTypeOf(fileBaseName);
        List<ChecklistItem> items = new ArrayList<>();

        String currentCategory = null;
        ChecklistItem current = null;
        StringBuilder hint = new StringBuilder();
        int seq = 0;

        for (String raw : markdown.split("\n", -1)) {
            String line = raw.strip();

            if (line.startsWith("### ")) {
                current = flush(items, current, hint);        // 직전 항목 마감
                String heading = line.substring(4).strip();
                Severity severity = severityOf(heading);
                if (severity == null) {                       // 판정어휘 없는 ### 는 검사항목 아님
                    continue;
                }
                current = new ChecklistItem();
                current.setArtifactType(artifactType);
                current.setCategory(currentCategory);
                current.setSeverity(severity);
                current.setDescription(stripVerdict(heading));
                current.setItemKey(fileBaseName + "-" + String.format("%02d", ++seq));
                hint.setLength(0);
            } else if (line.startsWith("## ")) {
                current = flush(items, current, hint);        // 섹션 경계에서 직전 항목 마감
                currentCategory = stripSectionNumber(line.substring(3).strip());
            } else if (current != null && line.startsWith("- ")) {
                if (hint.length() > 0) {
                    hint.append('\n');
                }
                hint.append(line.substring(2).strip());
            }
        }
        flush(items, current, hint);
        return items;
    }

    /** 진행 중이던 항목이 있으면 ruleHint를 확정해 목록에 추가하고 null 반환. */
    private ChecklistItem flush(List<ChecklistItem> items, ChecklistItem current, StringBuilder hint) {
        if (current == null) {
            return null;
        }
        if (hint.length() > 0) {
            current.setRuleHint(hint.toString());
        }
        items.add(current);
        return null;
    }

    /** 파일명(확장자 제외) 이 대응하는 ArtifactType. 교차정합성 등 매핑 없는 체크리스트는 null. */
    private ArtifactType artifactTypeOf(String fileBaseName) {
        for (ArtifactType type : ArtifactType.values()) {
            if (fileBaseName.equals(type.getChecklistKey())) {
                return type;
            }
        }
        return null;
    }

    private Severity severityOf(String heading) {
        if (heading.contains("[개선]")) {
            return Severity.IMPROVEMENT;
        }
        if (heading.contains("[권고]")) {
            return Severity.RECOMMENDATION;
        }
        return null;
    }

    private String stripVerdict(String heading) {
        return heading.replace("[개선]", "").replace("[권고]", "").strip();
    }

    private String stripSectionNumber(String section) {
        // "1. 파일명 검증" → "파일명 검증"
        return section.replaceFirst("^\\d+\\.?\\s*", "").strip();
    }
}
