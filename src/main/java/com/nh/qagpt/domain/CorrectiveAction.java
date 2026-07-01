package com.nh.qagpt.domain;

import com.nh.qagpt.domain.enums.ActionStatus;
import com.nh.qagpt.domain.enums.DefectType;
import com.nh.qagpt.domain.enums.Severity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 시정조치관리대장 라인 (PoC 스키마: 본문 17열 = 부적합사항 11 + 시정조치 계획 4 + 확인 2).
 * No는 CA_P##(개선)/CA_W##(권고), 검토자는 "AI품질검토봇" (spec §4.4).
 */
@Entity
@Table(name = "corrective_action")
@Getter
@Setter
@NoArgsConstructor
public class CorrectiveAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_result_id")
    private ReviewResult reviewResult;

    // --- 부적합사항 (11) ---
    private String no;                 // CA_P## / CA_W##
    private String businessName;       // 업무명
    @Enumerated(EnumType.STRING)
    private Severity improvementType;  // 개선 유형 (개선/권고)
    private LocalDate reviewDate;      // 검토일
    private String reviewer = "AI품질검토봇"; // 검토자 (고정)
    private String artifactName;       // 산출물명
    private String fileName;           // 파일명
    private String nonconformityLocation; // 부적합 위치
    @Enumerated(EnumType.STRING)
    private DefectType defectType;     // 결함유형
    @Column(columnDefinition = "text")
    private String nonconformityContent;  // 결함내용

    // --- 시정조치 계획 (4) ---
    @Column(columnDefinition = "text")
    private String actionPlan;         // 시정조치 계획
    private String plannedDate;        // 계획 일자
    private String assignee;           // 담당자
    @Enumerated(EnumType.STRING)
    private ActionStatus status = ActionStatus.TARGET;

    // --- 시정조치 확인 (2) ---
    @Column(columnDefinition = "text")
    private String confirmation;       // 확인 내용
    private LocalDate confirmedDate;   // 확인 일자
}
