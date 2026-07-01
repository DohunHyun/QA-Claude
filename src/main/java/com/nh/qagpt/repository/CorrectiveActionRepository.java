package com.nh.qagpt.repository;

import com.nh.qagpt.domain.CorrectiveAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CorrectiveActionRepository extends JpaRepository<CorrectiveAction, Long> {
    List<CorrectiveAction> findByReviewResultId(Long reviewResultId);
}
