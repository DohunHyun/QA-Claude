package com.nh.qagpt.controller;

import com.nh.qagpt.domain.ReviewResult;
import com.nh.qagpt.dto.ReviewResponse;
import com.nh.qagpt.exception.ResourceNotFoundException;
import com.nh.qagpt.repository.DefectRepository;
import com.nh.qagpt.repository.ReviewResultRepository;
import com.nh.qagpt.service.ReviewOrchestrator;
import com.nh.qagpt.service.generator.ResultGenerator;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewOrchestrator orchestrator;
    private final ReviewResultRepository reviewResultRepository;
    private final DefectRepository defectRepository;
    private final ResultGenerator resultGenerator;

    public ReviewController(ReviewOrchestrator orchestrator,
                            ReviewResultRepository reviewResultRepository,
                            DefectRepository defectRepository,
                            ResultGenerator resultGenerator) {
        this.orchestrator = orchestrator;
        this.reviewResultRepository = reviewResultRepository;
        this.defectRepository = defectRepository;
        this.resultGenerator = resultGenerator;
    }

    /** 검토 트리거 — 백그라운드(reviewExecutor)에서 4-Phase 검증 실행. 즉시 202 응답. */
    @PostMapping
    public ResponseEntity<Map<String, Object>> trigger(@RequestParam Long documentId) {
        orchestrator.review(documentId);
        return ResponseEntity.accepted().body(Map.of(
                "message", "검증 접수됨",
                "documentId", documentId));
    }

    @GetMapping("/{id}")
    public ReviewResponse get(@PathVariable Long id) {
        return reviewResultRepository.findById(id)
                .map(r -> ReviewResponse.from(r, defectRepository.countByReviewResultId(r.getId())))
                .orElseThrow(() -> new ResourceNotFoundException("검토 결과 없음: " + id));
    }

    /** [S4] 시정조치관리대장(.xlsx) 다운로드. lazy 연관(project/document/defects) 로드 위해 트랜잭션 내 생성. */
    @GetMapping("/{id}/corrective-action-ledger")
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> downloadCorrectiveActionLedger(@PathVariable Long id) {
        ReviewResult result = reviewResultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("검토 결과 없음: " + id));

        byte[] xlsx = resultGenerator.generateCorrectiveActionLedger(result);
        return xlsxResponse(xlsx, "시정조치관리대장_" + id + ".xlsx");
    }

    /**
     * [S5] AI 개선 산출물(.xlsx) 다운로드. 원본 파일 저장 연동 전이므로 원본을 함께 업로드받아
     * 개선(ERROR) 위치에 [개선] 태그를 달아 반환한다(단계별 1개).
     */
    @PostMapping(value = "/{id}/improved-artifact", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> downloadImprovedArtifact(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {
        ReviewResult result = reviewResultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("검토 결과 없음: " + id));

        byte[] improved = resultGenerator.generateImprovedArtifact(
                result, file.getBytes(), file.getOriginalFilename());

        String base = file.getOriginalFilename() == null ? "artifact.xlsx" : file.getOriginalFilename();
        return xlsxResponse(improved, "개선_" + base);
    }

    private ResponseEntity<Resource> xlsxResponse(byte[] bytes, String filename) {
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new ByteArrayResource(bytes));
    }
}
