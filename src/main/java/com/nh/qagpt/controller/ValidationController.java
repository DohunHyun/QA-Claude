package com.nh.qagpt.controller;

import com.nh.qagpt.domain.Document;
import com.nh.qagpt.domain.ReviewResult;
import com.nh.qagpt.domain.enums.ArtifactType;
import com.nh.qagpt.dto.DefectDto;
import com.nh.qagpt.dto.ValidationResultResponse;
import com.nh.qagpt.repository.DocumentRepository;
import com.nh.qagpt.repository.ProjectRepository;
import com.nh.qagpt.service.ReviewOrchestrator;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * [S2] 산출물 업로드 → 파싱 → (유형 자동 인식) → 규칙검증 + LLM 판정 → 결함 목록 반환 (동기).
 * projectId·artifactType은 선택 — artifactType 미지정 시 파일명/구조로 유형을 자동 인식한다.
 */
@RestController
@RequestMapping("/api")
public class ValidationController {

    private final DocumentRepository documentRepository;
    private final ProjectRepository projectRepository;
    private final ReviewOrchestrator orchestrator;

    public ValidationController(DocumentRepository documentRepository,
                                ProjectRepository projectRepository,
                                ReviewOrchestrator orchestrator) {
        this.documentRepository = documentRepository;
        this.projectRepository = projectRepository;
        this.orchestrator = orchestrator;
    }

    @PostMapping(value = "/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ValidationResultResponse validate(
            @RequestParam(required = false) Long projectId,
            @RequestParam(value = "artifactType", required = false) ArtifactType artifactType,
            @RequestParam("file") MultipartFile file) throws IOException {

        Document doc = new Document();
        if (projectId != null) {
            projectRepository.findById(projectId).ifPresent(doc::setProject);
        }
        doc.setFileName(file.getOriginalFilename());
        doc.setContentType(file.getContentType());
        if (artifactType != null) {
            doc.setArtifactType(artifactType);
            doc.setStage(artifactType.getStage());
        }
        Document saved = documentRepository.save(doc);

        // artifactType 미지정 시 orchestrator가 자동 인식하여 doc에 채운다.
        ReviewResult result = orchestrator.runReview(saved, file.getBytes(), artifactType);

        ArtifactType detected = saved.getArtifactType();
        List<DefectDto> defects = result.getDefects().stream().map(DefectDto::from).toList();
        String message = result.isPassed() ? "결함 없음" : (defects.size() + "건의 결함이 발견되었습니다.");
        return new ValidationResultResponse(
                result.getId(),
                detected == null ? null : detected.name(),
                detected == null ? null : detected.getLabel(),
                result.isPassed(),
                defects.size(),
                message,
                defects);
    }
}
