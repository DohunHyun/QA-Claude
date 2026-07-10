package com.nh.qagpt.repository;

import com.nh.qagpt.domain.Defect;
import com.nh.qagpt.domain.enums.Severity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DefectRepository extends JpaRepository<Defect, Long> {
    int countByReviewResultId(Long reviewResultId);

    /** 특정 검토의 심각도별 결함 수 (멈춘 검토 정리 시 통과 여부 판정용). */
    long countByReviewResultIdAndSeverity(Long reviewResultId, Severity severity);

    /** [S4] 시정조치대장 생성 시 결함 라인 로드 (LazyInitialization 회피). */
    List<Defect> findByReviewResultId(Long reviewResultId);
}
