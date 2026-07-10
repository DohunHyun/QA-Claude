package com.nh.qagpt.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * 단계말 검토결과서 발급 요청/승인.
 * PM이 프로젝트+단계그룹(예: 분석/설계) 단위로 발급을 요청하면 QA가 승인한다.
 * 승인(APPROVED) 상태여야 검토결과서(HWPX) 발급 버튼이 활성화된다.
 */
@Entity
@Table(name = "report_request")
@Getter
@Setter
@NoArgsConstructor
public class ReportRequest {

    public static final String REQUESTED = "REQUESTED";
    public static final String APPROVED = "APPROVED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    private String stageGroup;   // 예: "분석/설계"

    private String status = REQUESTED;

    private String requestedBy;
    @CreationTimestamp
    private Instant requestedAt;

    private String approvedBy;
    private Instant approvedAt;
}
