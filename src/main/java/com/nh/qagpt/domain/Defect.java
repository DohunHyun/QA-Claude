package com.nh.qagpt.domain;

import com.nh.qagpt.domain.enums.DefectType;
import com.nh.qagpt.domain.enums.Perspective;
import com.nh.qagpt.domain.enums.Severity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 개별 결함 항목. 심각도(개선/권고) + 결함유형(6종) + 관점(산출물/프로세스)을 태깅하고,
 * 반드시 산출물 내 근거 위치(시트/행/열/ID)를 명시한다 (spec §3, AI 동작 원칙 ⑤).
 */
@Entity
@Table(name = "defect")
@Getter
@Setter
@NoArgsConstructor
public class Defect {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_result_id")
    private ReviewResult reviewResult;

    @Enumerated(EnumType.STRING)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    private DefectType defectType;

    @Enumerated(EnumType.STRING)
    private Perspective perspective;

    private String checklistItemKey;   // 근거 체크리스트 항목 키

    // --- 근거 위치 (필수) ---
    private String locationSheet;
    private String locationRow;
    private String locationColumn;
    private String locationId;

    @Column(columnDefinition = "text")
    private String description;        // 결함 내용

    @Column(columnDefinition = "text")
    private String improvementGuide;   // 개선 방향
}
