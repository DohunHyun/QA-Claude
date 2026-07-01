package com.nh.qagpt.repository;

import com.nh.qagpt.domain.ChecklistItem;
import com.nh.qagpt.domain.enums.ArtifactType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, Long> {

    /** 유형별 체크리스트 항목 (checklist 엔진이 산출물 유형으로 조회). */
    List<ChecklistItem> findByArtifactType(ArtifactType artifactType);
}
