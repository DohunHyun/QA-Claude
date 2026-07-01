package com.nh.qagpt.controller;

import com.nh.qagpt.domain.Document;
import com.nh.qagpt.domain.Project;
import com.nh.qagpt.dto.UploadResponse;
import com.nh.qagpt.exception.ResourceNotFoundException;
import com.nh.qagpt.repository.DocumentRepository;
import com.nh.qagpt.repository.ProjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
public class DocumentUploadController {

    private final DocumentRepository documentRepository;
    private final ProjectRepository projectRepository;

    public DocumentUploadController(DocumentRepository documentRepository,
                                    ProjectRepository projectRepository) {
        this.documentRepository = documentRepository;
        this.projectRepository = projectRepository;
    }

    /** 산출물 업로드 (다중 업로드는 클라이언트가 파일당 호출). 유형/단계는 검토 시 classifier가 채운다. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> upload(@RequestParam Long projectId,
                                                 @RequestParam("file") MultipartFile file) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("프로젝트 없음: " + projectId));

        Document doc = new Document();
        doc.setProject(project);
        doc.setFileName(file.getOriginalFilename());
        doc.setContentType(file.getContentType());
        doc.setStoragePath("TODO"); // TODO: 파일 바이트 저장(로컬/스토리지) 후 경로 기록
        Document saved = documentRepository.save(doc);

        return ResponseEntity.status(HttpStatus.CREATED).body(UploadResponse.from(saved));
    }
}
