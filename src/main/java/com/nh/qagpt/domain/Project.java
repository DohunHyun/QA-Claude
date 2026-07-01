package com.nh.qagpt.domain;

import com.nh.qagpt.domain.enums.ProjectStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;

/**
 * PM이 등록하는 프로젝트. 명명규칙 검증의 1순위 기준값(코드·명·단계별 기간)을 담는다 (spec §명명규칙).
 * 프로젝트코드 자릿수·형식은 가변으로 전제한다(하드코딩 금지).
 */
@Entity
@Table(name = "project")
@Getter
@Setter
@NoArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;   // 프로젝트명
    private String code;   // 프로젝트코드 (예: NBIA)

    // 단계별 기간 — 파일 제·개정일자가 이 범위 내인지 검증할 때 사용
    private LocalDate managementStart;
    private LocalDate managementEnd;
    private LocalDate analysisStart;
    private LocalDate analysisEnd;
    private LocalDate designStart;
    private LocalDate designEnd;

    @Enumerated(EnumType.STRING)
    private ProjectStatus status = ProjectStatus.REQUESTED;

    @CreationTimestamp
    private Instant createdAt;
}
