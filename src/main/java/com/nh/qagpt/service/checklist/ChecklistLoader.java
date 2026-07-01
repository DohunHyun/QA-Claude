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
 *   <li>항목 아래의 본문(불릿·표 행·문단 등 비어있지 않은 줄)은 모두 ruleHint로 수집.
 *       필수컬럼 명세처럼 표로만 기술된 항목도 규칙을 놓치지 않는다. 빈 줄·수평선(---)은 제외</li>
 *   <li>판정어휘([개선]/[권고]) 없는 {@code ###}, 항목 밖(요약 표 등)의 내용은 무시</li>
 * </ul>
 *
 * phase·defectType·perspective 는 체크리스트에 항목별로 명시돼 있지 않으므로 비워둔다.
 * (개별 결함의 결함유형·관점 태깅은 판정 단계(S2)의 몫 — 시더는 "검사 정의"만 충실히 적재한다.)
 */
@Component
public class ChecklistLoader {

    public List<ChecklistItem> parse(String markdown, String fileBaseName) {
        ArtifactType artifactType = ArtifactType.fromChecklistKey(fileBaseName);
        List<ChecklistItem> items = new ArrayList<>();

        String currentCategory = null;
        ChecklistItem current = null;
        StringBuilder hint = new StringBuilder();
        int seq = 0;

        for (String raw : markdown.split("\n", -1)) {
            String line = raw.strip();

            if (line.startsWith("### ")) {
                commit(items, current, hint);                 // 직전 항목 마감
                current = null;
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
                commit(items, current, hint);                 // 섹션 경계에서 직전 항목 마감
                current = null;
                currentCategory = stripSectionNumber(line.substring(3).strip());
            } else if (current != null && isRuleContent(line)) {
                if (hint.length() > 0) {
                    hint.append('\n');
                }
                hint.append(line);                            // 불릿·표 행·문단 원문 유지
            }
        }
        commit(items, current, hint);
        return items;
    }

    /** 진행 중이던 항목이 있으면 수집한 ruleHint를 확정해 목록에 추가한다. */
    private void commit(List<ChecklistItem> items, ChecklistItem current, StringBuilder hint) {
        if (current == null) {
            return;
        }
        if (hint.length() > 0) {
            current.setRuleHint(hint.toString());
        }
        items.add(current);
    }

    // ruleHint에 담을 본문 줄인가. 빈 줄과 수평선(--- 등)은 제외.
    private boolean isRuleContent(String line) {
        return !line.isBlank() && !line.matches("^[-*_]{3,}$");
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
