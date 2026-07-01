package com.nh.qagpt.repository;

import com.nh.qagpt.domain.Defect;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DefectRepository extends JpaRepository<Defect, Long> {
    int countByReviewResultId(Long reviewResultId);
}
