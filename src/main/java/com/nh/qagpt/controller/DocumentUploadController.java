package com.nh.qagpt.controller;

import com.nh.qagpt.domain.Document;
import com.nh.qagpt.domain.Project;
import com.nh.qagpt.dto.UploadResponse;
import com.nh.qagpt.exception.ResourceNotFoundException;
import com.nh.qagpt.repository.DocumentRepository;
import com.nh.qagpt.repository.ProjectRepository;
import com.nh.qagpt.service.storage.FileStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentUploadController {

    private final DocumentRepository documentRepository;
    private final ProjectRepository projectRepository;
    private final FileStorageService fileStorage;

    public DocumentUploadController(DocumentRepository documentRepository,
                                    ProjectRepository projectRepository,
                                    FileStorageService fileStorage) {
        this.documentRepository = documentRepository;
        this.projectRepository = projectRepository;
        this.fileStorage = fileStorage;
    }

    /**
     * 산출물 업로드 — 혼합 다중 업로드 지원 (spec §4.1). 원본 바이트를 저장(storagePath)해
     * 비동기 검증·개선 산출물 생성이 재업로드 없이 가능하다. 유형/단계는 검토 시 classifier가 채운다.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<UploadResponse>> upload(@RequestParam Long projectId,
                                                       @RequestParam("file") List<MultipartFile> files)
            throws IOException {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("프로젝트 없음: " + projectId));

        List<UploadResponse> responses = new ArrayList<>();
        for (MultipartFile file : files) {
            Document doc = new Document();
            doc.setProject(project);
            doc.setFileName(file.getOriginalFilename());
            doc.setContentType(file.getContentType());
            doc.setStoragePath(fileStorage.store(file.getBytes(), file.getOriginalFilename()));
            responses.add(UploadResponse.from(documentRepository.save(doc)));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }
}
