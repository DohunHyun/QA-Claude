package com.nh.qagpt.repository;

import com.nh.qagpt.domain.ReviewResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewResultRepository extends JpaRepository<ReviewResult, Long> {
    List<ReviewResult> findByProjectIdOrderByRoundAsc(Long projectId);
    List<ReviewResult> findByDocumentId(Long documentId);
}
