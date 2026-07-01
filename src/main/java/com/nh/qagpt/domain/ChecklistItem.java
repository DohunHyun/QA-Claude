package com.nh.qagpt.domain;

import com.nh.qagpt.domain.enums.ArtifactType;
import com.nh.qagpt.domain.enums.DefectType;
import com.nh.qagpt.domain.enums.Perspective;
import com.nh.qagpt.domain.enums.Severity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 산출물별 체크리스트 항목. docs/checklists/ 12종을 DB로 적재해 checklist 엔진이 적용한다 (spec §5).
 * phase는 4-Phase 검증 실행 순서(1~4)를 가리킨다.
 */
@Entity
@Table(name = "checklist_item")
@Getter
@Setter
@NoArgsConstructor
public class ChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ArtifactType artifactType;

    private int phase;   // 1~4 (4-Phase)

    @Enumerated(EnumType.STRING)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    private DefectType defectType;

    @Enumerated(EnumType.STRING)
    private Perspective perspective;

    private String category;   // 파일/표지/개정/본문/일관성 등
    private String itemKey;    // 고유 키

    @Column(columnDefinition = "text")
    private String description; // 검사 내용

    @Column(columnDefinition = "text")
    private String ruleHint;    // 검증 규칙 힌트 (LLM 프롬프트 보강용)
}
