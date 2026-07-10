package com.nh.qagpt.controller;

import com.nh.qagpt.domain.Document;
import com.nh.qagpt.domain.Project;
import com.nh.qagpt.domain.ReportRequest;
import com.nh.qagpt.domain.ReviewResult;
import com.nh.qagpt.domain.enums.ArtifactType;
import com.nh.qagpt.domain.enums.Stage;
import com.nh.qagpt.dto.ReportRequestDto;
import com.nh.qagpt.exception.ResourceNotFoundException;
import com.nh.qagpt.repository.ProjectRepository;
import com.nh.qagpt.repository.ReportRequestRepository;
import com.nh.qagpt.repository.ReviewResultRepository;
import com.nh.qagpt.service.generator.HwpxReviewReportWriter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * 단계말 검토결과서 발급 요청/승인 API.
 * PM 요청(REQUESTED) → QA 승인(APPROVED) → 승인건에 한해 집계 HWPX 발급.
 */
@RestController
@RequestMapping("/api/report-requests")
public class ReportRequestController {

    private static final Map<String, List<Stage>> GROUP_STAGES = Map.of(
            "분석/설계", List.of(Stage.ANALYSIS, Stage.DESIGN),
            "테스트/이행", List.of());

    private final ReportRequestRepository requestRepo;
    private final ProjectRepository projectRepo;
    private final ReviewResultRepository reviewRepo;
    private final HwpxReviewReportWriter writer = new HwpxReviewReportWriter();

    public ReportRequestController(ReportRequestRepository requestRepo, ProjectRepository projectRepo,
                                   ReviewResultRepository reviewRepo) {
        this.requestRepo = requestRepo;
        this.projectRepo = projectRepo;
        this.reviewRepo = reviewRepo;
    }

    /** 발급 요청 생성 (PM). body: projectId, stageGroup, requestedBy */
    @PostMapping
    @Transactional
    public ReportRequestDto create(@RequestBody Map<String, String> body) {
        Long projectId = Long.valueOf(body.get("projectId"));
        Project project = projectRepo.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("프로젝트 없음: " + projectId));
        ReportRequest r = new ReportRequest();
        r.setProject(project);
        r.setStageGroup(body.getOrDefault("stageGroup", "분석/설계"));
        r.setStatus(ReportRequest.REQUESTED);
        r.setRequestedBy(body.get("requestedBy"));
        return ReportRequestDto.from(requestRepo.save(r));
    }

    /** 목록 — status(예: REQUESTED) 또는 projectId+stageGroup 으로 필터. */
    @GetMapping
    @Transactional(readOnly = true)
    public List<ReportRequestDto> list(@RequestParam(required = false) String status,
                                       @RequestParam(required = false) Long projectId,
                                       @RequestParam(required = false) String stageGroup) {
        List<ReportRequest> rows;
        if (status != null) {
            rows = requestRepo.findByStatusOrderByRequestedAtDesc(status);
        } else if (projectId != null && stageGroup != null) {
            rows = requestRepo.findByProjectIdAndStageGroupOrderByIdDesc(projectId, stageGroup);
        } else {
            rows = requestRepo.findAllByOrderByIdDesc();
        }
        return rows.stream().map(ReportRequestDto::from).toList();
    }

    /** QA 승인. body: approvedBy */
    @PostMapping("/{id}/approve")
    @Transactional
    public ReportRequestDto approve(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        ReportRequest r = requestRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("발급 요청 없음: " + id));
        r.setStatus(ReportRequest.APPROVED);
        r.setApprovedBy(body == null ? null : body.get("approvedBy"));
        r.setApprovedAt(Instant.now());
        return ReportRequestDto.from(requestRepo.save(r));
    }

    /** 승인된 요청의 집계 검토결과서(.hwpx) 다운로드. */
    @GetMapping("/{id}/review-report")
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        ReportRequest req = requestRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("발급 요청 없음: " + id));
        if (!ReportRequest.APPROVED.equals(req.getStatus())) {
            throw new IllegalStateException("QA 승인 후 검토결과서를 발급할 수 있습니다.");
        }
        Project project = req.getProject();
        List<Stage> stages = GROUP_STAGES.getOrDefault(req.getStageGroup(), List.of());

        // 프로젝트의 해당 단계 검토 회차 → 산출물별 최신 회차만
        Map<ArtifactType, ReviewResult> latest = new LinkedHashMap<>();
        for (ReviewResult r : reviewRepo.findByProjectIdOrderByRoundAsc(project.getId())) {
            if (r.getStage() == null || !stages.contains(r.getStage())) continue;
            Document doc = r.getDocument();
            ArtifactType at = doc == null ? ArtifactType.UNKNOWN : doc.getArtifactType();
            latest.put(at, r);   // 라운드 오름차순이라 최신이 마지막에 덮어씀
        }
        List<ReviewResult> reviews = new ArrayList<>(latest.values());
        reviews.sort(Comparator.comparing((ReviewResult r) -> r.getStage() == null ? "" : r.getStage().name())
                .thenComparing(r -> {
                    Document d = r.getDocument();
                    return d == null || d.getArtifactType() == null ? "" : d.getArtifactType().name();
                }));

        LocalDate approvedDate = req.getApprovedAt() == null ? null
                : req.getApprovedAt().atZone(ZoneId.systemDefault()).toLocalDate();
        String approver = req.getApprovedBy() == null ? "" : req.getApprovedBy();

        byte[] hwpx = writer.writeAggregate(project, req.getStageGroup(), reviews, approver, approvedDate);

        String filename = "단계말검토결과서_" + safe(project.getCode()) + "_" + id + ".hwpx";
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType("application/hwp+zip"))
                .body(new ByteArrayResource(hwpx));
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
