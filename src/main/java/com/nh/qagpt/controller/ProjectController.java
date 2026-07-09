package com.nh.qagpt.controller;

import com.nh.qagpt.domain.Project;
import com.nh.qagpt.dto.ProjectRequest;
import com.nh.qagpt.dto.ProjectResponse;
import com.nh.qagpt.exception.ResourceNotFoundException;
import com.nh.qagpt.repository.ProjectRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectRepository projectRepository;

    public ProjectController(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody ProjectRequest req) {
        Project p = new Project();
        p.setName(req.name());
        p.setCode(req.code());
        p.setManagementStart(req.managementStart());
        p.setManagementEnd(req.managementEnd());
        p.setAnalysisStart(req.analysisStart());
        p.setAnalysisEnd(req.analysisEnd());
        p.setDesignStart(req.designStart());
        p.setDesignEnd(req.designEnd());
        Project saved = projectRepository.save(p);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProjectResponse.from(saved));
    }

    /** 프로젝트 목록 — 검증 업로드 등 프론트 드롭다운이 실제 DB 프로젝트 id를 쓰도록 제공. */
    @GetMapping
    public List<ProjectResponse> list() {
        return projectRepository.findAll().stream().map(ProjectResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ProjectResponse get(@PathVariable Long id) {
        return projectRepository.findById(id)
                .map(ProjectResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("프로젝트 없음: " + id));
    }

    /** [spec §7.1-2] 관리자 승인 — 프로젝트 활성화(검증 절차 시작 가능). */
    @PostMapping("/{id}/approve")
    public ProjectResponse approve(@PathVariable Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("프로젝트 없음: " + id));
        project.setStatus(com.nh.qagpt.domain.enums.ProjectStatus.ACTIVE);
        return ProjectResponse.from(projectRepository.save(project));
    }
}
