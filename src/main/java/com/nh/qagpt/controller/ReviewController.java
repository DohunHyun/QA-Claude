package com.nh.qagpt.controller;

import com.nh.qagpt.domain.Document;
import com.nh.qagpt.domain.ReviewResult;
import com.nh.qagpt.dto.ReviewResponse;
import com.nh.qagpt.exception.QaApprovalException;
import com.nh.qagpt.exception.ResourceNotFoundException;
import com.nh.qagpt.repository.DefectRepository;
import com.nh.qagpt.repository.ReviewResultRepository;
import com.nh.qagpt.service.ReviewOrchestrator;
import com.nh.qagpt.service.generator.ResultGenerator;
import com.nh.qagpt.service.storage.FileStorageService;
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
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewOrchestrator orchestrator;
    private final ReviewResultRepository reviewResultRepository;
    private final DefectRepository defectRepository;
    private final ResultGenerator resultGenerator;
    private final FileStorageService fileStorage;

    public ReviewController(ReviewOrchestrator orchestrator,
                            ReviewResultRepository reviewResultRepository,
                            DefectRepository defectRepository,
                            ResultGenerator resultGenerator,
                            FileStorageService fileStorage) {
        this.orchestrator = orchestrator;
        this.reviewResultRepository = reviewResultRepository;
        this.defectRepository = defectRepository;
        this.resultGenerator = resultGenerator;
        this.fileStorage = fileStorage;
    }

    /** 검토 트리거 — 백그라운드(reviewExecutor)에서 4-Phase 검증 실행. 즉시 202 응답 (spec §10.1). */
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

    /** 비동기 검증 진행도 폴링 — 산출물 기준 검토 목록(RUNNING→COMPLETED/FAILED). */
    @GetMapping("/by-document/{documentId}")
    public List<ReviewResponse> byDocument(@PathVariable Long documentId) {
        return reviewResultRepository.findByDocumentId(documentId).stream()
                .map(r -> ReviewResponse.from(r, defectRepository.countByReviewResultId(r.getId())))
                .toList();
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
     * [S5] AI 개선 산출물(.xlsx) 다운로드 — 저장된 원본(storagePath)으로부터 생성 (재업로드 불필요).
     * 개선(ERROR) 위치에 [개선] 태그를 달아 반환한다(단계별 1개, spec §4.3).
     */
    @GetMapping("/{id}/improved-artifact")
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> downloadImprovedArtifactFromStorage(@PathVariable Long id) {
        ReviewResult result = reviewResultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("검토 결과 없음: " + id));
        Document doc = result.getDocument();
        if (doc == null || !fileStorage.exists(doc.getStoragePath())) {
            throw new ResourceNotFoundException("저장된 원본이 없습니다. 원본 파일과 함께 POST로 요청하세요.");
        }
        byte[] original = fileStorage.load(doc.getStoragePath());
        byte[] improved = resultGenerator.generateImprovedArtifact(result, original, doc.getFileName());

        String base = doc.getFileName() == null ? "artifact.xlsx" : doc.getFileName();
        return xlsxResponse(improved, "개선_" + base);
    }

    /** [S5] AI 개선 산출물(.xlsx) 다운로드 — 원본을 함께 업로드하는 폴백 경로. */
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

    /**
     * [S7] QA 승인. 개선 잔존(passed=false)인데 exception=false면 예외 승인 경로를 요구(409).
     * exception=true면 QA 예외 승인으로 통과 처리.
     */
    @PostMapping("/{id}/approve")
    public Map<String, Object> approve(@PathVariable Long id,
                                       @RequestParam(defaultValue = "false") boolean exception) {
        ReviewResult result = reviewResultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("검토 결과 없음: " + id));

        if (!result.isPassed() && !exception) {
            throw new QaApprovalException(
                    "개선 항목이 잔존합니다. QA 예외 승인이 필요합니다(exception=true).");
        }
        result.setQaApproved(true);
        result.setQaException(exception);
        reviewResultRepository.save(result);
        return Map.of(
                "reviewId", id,
                "qaApproved", true,
                "qaException", exception,
                "message", exception ? "QA 예외 승인 완료" : "QA 승인 완료");
    }

    /** [S7] 단계말 검토결과서(.hwpx) 다운로드. QA 승인(qaApproved) 이후에만 발급된다. */
    @GetMapping("/{id}/review-report")
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> downloadReviewReport(@PathVariable Long id) {
        ReviewResult result = reviewResultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("검토 결과 없음: " + id));
        if (!result.isQaApproved()) {
            throw new QaApprovalException("QA 승인 후 검토결과서를 발급할 수 있습니다.");
        }
        byte[] hwpx = resultGenerator.generateReviewReport(result);

        String filename = "단계말검토결과서_" + id + ".hwpx";
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType("application/hwp+zip"))
                .body(new ByteArrayResource(hwpx));
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
