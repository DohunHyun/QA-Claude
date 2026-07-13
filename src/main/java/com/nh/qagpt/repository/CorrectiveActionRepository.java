package com.nh.qagpt.repository;

import com.nh.qagpt.domain.CorrectiveAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CorrectiveActionRepository extends JpaRepository<CorrectiveAction, Long> {
    List<CorrectiveAction> findByReviewResultId(Long reviewResultId);

    /** 프로젝트 전체 시정조치(모든 검토 회차) — 회차·라인 순으로. 프로젝트 대장 다운로드에 사용. */
    @Query("select a from CorrectiveAction a where a.reviewResult.project.id = :projectId "
            + "order by a.reviewResult.round asc, a.id asc")
    List<CorrectiveAction> findByProjectId(@Param("projectId") Long projectId);
}
