package com.nh.qagpt.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nh.qagpt.domain.Defect;
import com.nh.qagpt.domain.Document;
import com.nh.qagpt.domain.ReviewResult;
import com.nh.qagpt.domain.enums.ArtifactType;
import com.nh.qagpt.dto.DefectDto;
import com.nh.qagpt.repository.ReviewResultRepository;
import com.nh.qagpt.service.checklist.ArtifactSummary;
import com.nh.qagpt.service.checklist.CrossConsistencyChecker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * [S8/후속] 프로젝트 단위 교차 산출물 정합성 판정. 각 유형의 최신 회차 요약(rawResultJson)을 로드해
 * 산출물 간 관계(예: 배치Job목록 Job ID ⊆ 배치설계서 Job ID)를 검증한다.
 */
@Service
public class CrossConsistencyService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ReviewResultRepository reviewResultRepository;
    private final CrossConsistencyChecker checker = new CrossConsistencyChecker();

    public CrossConsistencyService(ReviewResultRepository reviewResultRepository) {
        this.reviewResultRepository = reviewResultRepository;
    }

    @Transactional(readOnly = true)
    public List<DefectDto> check(Long projectId) {
        // 회차 오름차순 → 유형별로 마지막(최신 회차) 요약이 맵에 남는다.
        Map<ArtifactType, ArtifactSummary> latest = new EnumMap<>(ArtifactType.class);
        for (ReviewResult r : reviewResultRepository.findByProjectIdOrderByRoundAsc(projectId)) {
            ArtifactType type = typeOf(r);
            ArtifactSummary summary = parse(r.getRawResultJson());
            if (type != null && type != ArtifactType.UNKNOWN && summary != null) {
                latest.put(type, summary);
            }
        }

        List<Defect> defects = new ArrayList<>();

        // 관계 1: 배치Job목록 Job ID ⊆ 배치설계서 Job ID (checklist_batch_job_list §5)
        ArtifactSummary batchList = latest.get(ArtifactType.BATCH_JOB_LIST);
        ArtifactSummary batchDesign = latest.get(ArtifactType.BATCH_DESIGN);
        if (batchList != null && batchDesign != null) {
            defects.addAll(checker.idSubsetCoverage(
                    idSet(batchList), idSet(batchDesign), "배치Job목록", "배치설계서"));
        }

        return defects.stream().map(DefectDto::from).toList();
    }

    private Set<String> idSet(ArtifactSummary s) {
        return new LinkedHashSet<>(s.ids());
    }

    private ArtifactSummary parse(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, ArtifactSummary.class);
        } catch (Exception e) {
            return null;
        }
    }

    private ArtifactType typeOf(ReviewResult r) {
        Document doc = r.getDocument();
        return doc == null ? null : doc.getArtifactType();
    }
}
