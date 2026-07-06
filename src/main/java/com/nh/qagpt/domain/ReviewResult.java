package com.nh.qagpt.domain;

import com.nh.qagpt.domain.enums.ReviewStatus;
import com.nh.qagpt.domain.enums.Stage;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** 검토 1회차 결과. 개선 항목이 하나라도 있으면 passed=false (QA 예외 승인 시 예외). */
@Entity
@Table(name = "review_result")
@Getter
@Setter
@NoArgsConstructor
public class ReviewResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private Document document;

    @Enumerated(EnumType.STRING)
    private Stage stage;

    private int round;   // 회차 (재검증마다 증가)

    @Enumerated(EnumType.STRING)
    private ReviewStatus status = ReviewStatus.PENDING;

    private boolean passed;

    private boolean qaException;  // QA 예외 승인으로 통과 처리되었는지

    private boolean qaApproved;   // [S7] QA 승인 완료(검토결과서 발급 가능 조건)

    @OneToMany(mappedBy = "reviewResult", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Defect> defects = new ArrayList<>();

    /** Claude 원본 응답 및 구조화 결과. PostgreSQL: jsonb, H2: text. */
    @Column(columnDefinition = "text")
    private String rawResultJson;

    @CreationTimestamp
    private Instant createdAt;

    public void addDefect(Defect defect) {
        defects.add(defect);
        defect.setReviewResult(this);
    }
}
