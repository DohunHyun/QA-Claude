package com.nh.qagpt.repository;

import com.nh.qagpt.domain.ReviewResult;
import com.nh.qagpt.domain.enums.ArtifactType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewResultRepository extends JpaRepository<ReviewResult, Long> {
    List<ReviewResult> findByProjectIdOrderByRoundAsc(Long projectId);
    List<ReviewResult> findByDocumentId(Long documentId);

    /** [S6] 같은 프로젝트·산출물 유형의 최대 회차 (재검증 시 회차 증가용). 없으면 0. */
    @Query("select coalesce(max(r.round), 0) from ReviewResult r "
            + "where r.project.id = :projectId and r.document.artifactType = :type")
    int maxRoundForArtifact(@Param("projectId") Long projectId, @Param("type") ArtifactType type);
}
