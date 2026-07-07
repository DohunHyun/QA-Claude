package com.nh.qagpt.repository;

import com.nh.qagpt.domain.Defect;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DefectRepository extends JpaRepository<Defect, Long> {
    int countByReviewResultId(Long reviewResultId);

    /** [S4] 시정조치대장 생성 시 결함 라인 로드 (LazyInitialization 회피). */
    List<Defect> findByReviewResultId(Long reviewResultId);
}
