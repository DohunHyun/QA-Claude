package com.nh.qagpt.controller;

import com.nh.qagpt.domain.Document;
import com.nh.qagpt.domain.ReviewResult;
import com.nh.qagpt.domain.enums.ArtifactType;
import com.nh.qagpt.dto.DefectDto;
import com.nh.qagpt.dto.ValidationResultResponse;
import com.nh.qagpt.repository.DocumentRepository;
import com.nh.qagpt.repository.ProjectRepository;
import com.nh.qagpt.service.ReviewOrchestrator;
import com.nh.qagpt.service.storage.FileStorageService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 산출물 업로드 → 파싱 → (유형 자동 인식) → 규칙검증 + LLM 판정 → 결함 목록 반환 (동기).
 * projectId·artifactType은 선택 — artifactType 미지정 시 파일명/구조로 유형을 자동 인식한다.
 * 원본 바이트는 저장(storagePath)되어 개선 산출물 생성이 재업로드 없이 가능하다.
 */
@RestController
@RequestMapping("/api")
public class ValidationController {

    private final DocumentRepository documentRepository;
    private final ProjectRepository projectRepository;
    private final ReviewOrchestrator orchestrator;
    private final FileStorageService fileStorage;

    public ValidationController(DocumentRepository documentRepository,
                                ProjectRepository projectRepository,
                                ReviewOrchestrator orchestrator,
                                FileStorageService fileStorage) {
        this.documentRepository = documentRepository;
        this.projectRepository = projectRepository;
        this.orchestrator = orchestrator;
        this.fileStorage = fileStorage;
    }

    @PostMapping(value = "/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ValidationResultResponse validate(
            @RequestParam(required = false) Long projectId,
            @RequestParam(value = "artifactType", required = false) ArtifactType artifactType,
            @RequestParam("file") MultipartFile file) throws IOException {
        return validateOne(projectId, artifactType, file);
    }

    /** [spec §4.1] 혼합 다중 업로드 검증 — 여러 파일을 한 번에 받아 각각 자동 인식·검증한다. */
    @PostMapping(value = "/validate-batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<ValidationResultResponse> validateBatch(
            @RequestParam(required = false) Long projectId,
            @RequestParam("files") List<MultipartFile> files) throws IOException {
        List<ValidationResultResponse> responses = new ArrayList<>();
        for (MultipartFile file : files) {
            responses.add(validateOne(projectId, null, file)); // 배치는 항상 자동 인식
        }
        return responses;
    }

    private ValidationResultResponse validateOne(Long projectId, ArtifactType artifactType,
                                                 MultipartFile file) throws IOException {
        byte[] content = file.getBytes();

        Document doc = new Document();
        if (projectId != null) {
            projectRepository.findById(projectId).ifPresent(doc::setProject);
        }
        doc.setFileName(file.getOriginalFilename());
        doc.setContentType(file.getContentType());
        doc.setStoragePath(fileStorage.store(content, file.getOriginalFilename())); // 원본 보관
        if (artifactType != null) {
            doc.setArtifactType(artifactType);
            doc.setStage(artifactType.getStage());
        }
        Document saved = documentRepository.save(doc);

        // artifactType 미지정 시 orchestrator가 자동 인식하여 doc에 채운다.
        ReviewResult result = orchestrator.runReview(saved, content, artifactType);

        // [S6] orchestrator가 채운 유형/단계를 영속화 (회차 집계·현황이 유형으로 조회하므로 필수).
        documentRepository.save(saved);

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
