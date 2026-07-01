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

    @GetMapping("/{id}")
    public ProjectResponse get(@PathVariable Long id) {
        return projectRepository.findById(id)
                .map(ProjectResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("프로젝트 없음: " + id));
    }
}
