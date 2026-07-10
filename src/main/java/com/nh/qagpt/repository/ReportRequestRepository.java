package com.nh.qagpt.repository;

import com.nh.qagpt.domain.ReportRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRequestRepository extends JpaRepository<ReportRequest, Long> {
    List<ReportRequest> findByStatusOrderByRequestedAtDesc(String status);
    List<ReportRequest> findByProjectIdAndStageGroupOrderByIdDesc(Long projectId, String stageGroup);
    List<ReportRequest> findAllByOrderByIdDesc();
}
