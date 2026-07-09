package com.nh.qagpt.config;

import com.nh.qagpt.domain.CorrectiveAction;
import com.nh.qagpt.domain.Defect;
import com.nh.qagpt.domain.Document;
import com.nh.qagpt.domain.Project;
import com.nh.qagpt.domain.ReviewResult;
import com.nh.qagpt.domain.enums.*;
import com.nh.qagpt.repository.CorrectiveActionRepository;
import com.nh.qagpt.repository.DocumentRepository;
import com.nh.qagpt.repository.ProjectRepository;
import com.nh.qagpt.repository.ReviewResultRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 기동 시 데모 데이터(프로젝트 3건 + 검토 회차 + 결함 + 시정조치 라인)를 시드한다.
 * 프론트 목업(assets/js/mock.js)의 PROJECTS/REVIEWS/DEFECTS 와 동형으로 맞춰
 * 실 API 화면(검증 현황·검증 결과)과 다운로드(시정조치관리대장·검토결과서)가
 * 목업 기반 화면들과 데이터 정합성을 갖도록 한다.
 *
 * <p>시드 조건은 "프로젝트가 하나도 없을 때"이다(H2 인메모리는 매 기동, PostgreSQL은 최초 1회).
 * 프로젝트가 없는데 남아있는 레거시 orphan 검토/문서/시정조치는 데모 정합성을 위해 먼저 정리한다.
 */
@Component
@Order(1)
public class DemoDataSeeder implements ApplicationRunner {

    private final ProjectRepository projectRepo;
    private final DocumentRepository documentRepo;
    private final ReviewResultRepository reviewRepo;
    private final CorrectiveActionRepository correctiveActionRepo;

    public DemoDataSeeder(ProjectRepository projectRepo, DocumentRepository documentRepo,
                          ReviewResultRepository reviewRepo,
                          CorrectiveActionRepository correctiveActionRepo) {
        this.projectRepo = projectRepo;
        this.documentRepo = documentRepo;
        this.reviewRepo = reviewRepo;
        this.correctiveActionRepo = correctiveActionRepo;
    }

    // 검토 회차: 프로젝트코드 | 산출물명 | 회차 | 상태 | 통과 | 개선수 | 권고수
    private static final String[] REVIEWS = {
        // NHOB — 관리·분석 완료, 설계 진행중
        "NHOB|문서작성지침서|1|COMPLETED|true|0|1",
        "NHOB|테일러링결과서|1|COMPLETED|true|0|0",
        "NHOB|요구사항추적표|2|COMPLETED|true|0|1",
        "NHOB|요구사항정의서|2|COMPLETED|true|0|2",
        "NHOB|배치Job목록|1|COMPLETED|true|0|1",
        "NHOB|프로세스정의서|1|COMPLETED|true|0|1",
        "NHOB|UI목록|1|RUNNING|false|0|0",
        "NHOB|배치설계서|2|COMPLETED|false|3|2",
        "NHOB|인터페이스설계서|1|COMPLETED|false|2|1",
        "NHOB|프로그램목록|1|COMPLETED|false|1|1",
        // NHUL — 분석 회차 다수, 설계 진행중
        "NHUL|문서작성지침서|1|COMPLETED|true|0|0",
        "NHUL|테일러링결과서|2|COMPLETED|true|0|1",
        "NHUL|요구사항정의서|1|COMPLETED|false|5|3",
        "NHUL|요구사항정의서|2|COMPLETED|false|2|2",
        "NHUL|요구사항정의서|3|COMPLETED|true|0|1",
        "NHUL|인터페이스정의서|1|COMPLETED|true|0|1",
        "NHUL|배치설계서|1|COMPLETED|false|4|2",
        "NHUL|배치설계서|2|RUNNING|false|0|0",
        "NHUL|UI목록|1|COMPLETED|false|2|3",
        // NHGBS — 전 단계 완료(통과)
        "NHGBS|문서작성지침서|1|COMPLETED|true|0|0",
        "NHGBS|테일러링결과서|1|COMPLETED|true|0|1",
        "NHGBS|요구사항추적표|1|COMPLETED|true|0|0",
        "NHGBS|요구사항정의서|2|COMPLETED|true|0|1",
        "NHGBS|프로세스정의서|1|COMPLETED|true|0|0",
        "NHGBS|배치Job목록|1|COMPLETED|true|0|1",
        "NHGBS|UI목록|1|COMPLETED|true|0|1",
        "NHGBS|프로그램목록|2|COMPLETED|true|0|0",
        "NHGBS|인터페이스설계서|1|COMPLETED|true|0|1",
        "NHGBS|배치설계서|1|COMPLETED|true|0|1"
    };

    // 개선 결함 내용/개선방향 — 결함유형(MISSING_REQUIRED/CONTENT_ERROR/STANDARD_VIOLATION/DUPLICATE)과 정렬
    private static final DefectType[] IMPROVE_TYPES = {
        DefectType.MISSING_REQUIRED, DefectType.CONTENT_ERROR, DefectType.STANDARD_VIOLATION, DefectType.DUPLICATE
    };
    private static final String[] IMPROVE_DESC = {
        "필수 항목이 누락되어 완전성 기준을 충족하지 못함",
        "기재 내용이 상호 모순되어 정확성 확인이 곤란함",
        "문서번호·버전 표기가 표준 명명규칙과 불일치함",
        "동일 항목이 2개 이상 중복 정의되어 추적성이 저하됨"
    };
    private static final String[] IMPROVE_GUIDE = {
        "누락 항목을 표준 양식에 맞게 보완",
        "모순되는 내용을 재확인하여 일치화",
        "표준 명명규칙(예: V1.0a)에 맞게 재저장",
        "중복 항목을 통합하고 ID를 재채번"
    };

    @Override
    public void run(ApplicationArguments args) {
        // 데모 프로젝트(NHOB)가 이미 있으면 시드 완료 상태 → 재시드 안 함.
        // (사용자 테스트로 생성된 다른 프로젝트가 있어도 데모 데이터는 채운다.)
        if (projectRepo.findByCode("NHOB").isPresent()) return;

        // 데모 프로젝트에 연결되지 않은 레거시 orphan 검토/문서/시정조치 정리 (데모 정합성)
        correctiveActionRepo.deleteAllInBatch();
        reviewRepo.deleteAll();     // cascade(orphanRemoval) → 결함 제거
        documentRepo.deleteAll();

        Map<String, Project> projects = new HashMap<>();
        projects.put("NHOB", seedProject("NHOB", "(은행) 국외지점 전산시스템 개선",
                LocalDate.of(2025, 11, 3), LocalDate.of(2026, 8, 31)));
        projects.put("NHUL", seedProject("NHUL", "(은행) U2L 전환 구축",
                LocalDate.of(2026, 1, 5), LocalDate.of(2026, 9, 30)));
        projects.put("NHGBS", seedProject("NHGBS", "(은행) GBS 여신시스템 구축",
                LocalDate.of(2025, 9, 1), LocalDate.of(2026, 6, 30)));

        Map<String, ArtifactType> byLabel = new HashMap<>();
        for (ArtifactType at : ArtifactType.values()) byLabel.put(at.getLabel(), at);

        for (String row : REVIEWS) {
            String[] c = row.split("\\|");
            Project proj = projects.get(c[0]);
            ArtifactType at = byLabel.get(c[1]);
            if (proj == null || at == null) continue;
            int round = Integer.parseInt(c[2]);
            ReviewStatus status = ReviewStatus.valueOf(c[3]);
            boolean passed = Boolean.parseBoolean(c[4]);
            int errors = Integer.parseInt(c[5]);
            int warns = Integer.parseInt(c[6]);

            Document doc = new Document();
            doc.setProject(proj);
            doc.setArtifactType(at);
            doc.setStage(at.getStage());
            doc.setFileName(c[0] + "-DV-" + at.getChecklistKey() + "-" + c[1] + "_V" + round + ".0a.xlsx");
            doc.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            documentRepo.save(doc);

            ReviewResult r = new ReviewResult();
            r.setProject(proj);
            r.setDocument(doc);
            r.setStage(at.getStage());
            r.setRound(round);
            r.setStatus(status);
            r.setPassed(passed);
            for (int i = 0; i < errors; i++) r.getDefects().add(defect(r, at, Severity.IMPROVEMENT, i));
            for (int i = 0; i < warns; i++) r.getDefects().add(defect(r, at, Severity.RECOMMENDATION, i));
            reviewRepo.save(r);

            seedCorrectiveActions(r, at);
        }
    }

    private Project seedProject(String code, String name, LocalDate start, LocalDate end) {
        Project p = new Project();
        p.setCode(code);
        p.setName(name);
        p.setStatus(ProjectStatus.ACTIVE);
        p.setManagementStart(start);
        p.setDesignEnd(end);
        return projectRepo.save(p);
    }

    private Defect defect(ReviewResult r, ArtifactType at, Severity sev, int idx) {
        Defect d = new Defect();
        d.setReviewResult(r);
        d.setSeverity(sev);
        // 일부 결함은 프로세스 관점으로 → 시정조치 No가 CA_P##/CA_W## 로 분화됨
        d.setPerspective(idx == 1 ? Perspective.PROCESS : Perspective.ARTIFACT);
        d.setLocationSheet(at.getStage() == Stage.DESIGN ? "설계" : "본문");
        d.setLocationRow(String.valueOf(10 + idx * 7));
        d.setLocationId(at.getChecklistKey() != null ? at.getChecklistKey().toUpperCase() + "-" + (idx + 1) : null);
        if (sev == Severity.IMPROVEMENT) {
            d.setDefectType(IMPROVE_TYPES[idx % IMPROVE_TYPES.length]);
            d.setDescription(at.getLabel() + " — " + IMPROVE_DESC[idx % IMPROVE_DESC.length]);
            d.setImprovementGuide(IMPROVE_GUIDE[idx % IMPROVE_GUIDE.length]);
        } else {
            d.setDefectType(DefectType.ETC);
            d.setDescription(at.getLabel() + " — 보완 권고 사항 #" + (idx + 1) + " (선택 항목 상세화 권장)");
            d.setImprovementGuide("권고 사항 반영 검토");
        }
        return d;
    }

    /** 검토 결과의 결함마다 시정조치 라인을 생성한다(서비스 createFromReview 와 동형). */
    private void seedCorrectiveActions(ReviewResult r, ArtifactType at) {
        LocalDate reviewDate = LocalDate.now();
        boolean earlyStage = at.getStage() != Stage.DESIGN;  // 관리·분석 → 조치완료, 설계 → 조치대상
        Document doc = r.getDocument();
        int pSeq = 0, wSeq = 0;
        for (Defect d : r.getDefects()) {
            boolean isProcess = d.getPerspective() == Perspective.PROCESS;
            CorrectiveAction a = new CorrectiveAction();
            a.setReviewResult(r);
            a.setNo(isProcess ? String.format("CA_P%02d", ++pSeq) : String.format("CA_W%02d", ++wSeq));
            a.setBusinessName(isProcess ? "프로젝트관리" : "개발산출물");
            a.setImprovementType(d.getSeverity());
            a.setReviewDate(reviewDate);
            a.setArtifactName(at.getLabel());
            a.setFileName(doc.getFileName());
            a.setNonconformityLocation(location(d));
            a.setDefectType(d.getDefectType());
            a.setNonconformityContent(d.getDescription());
            a.setActionPlan(d.getImprovementGuide());
            a.setAssignee("김명호");
            a.setPlannedDate(reviewDate.plusDays(5).toString());
            if (earlyStage) {
                a.setStatus(ActionStatus.DONE);
                a.setConfirmedDate(reviewDate.plusDays(3));
                a.setConfirmation("조치 완료 · QA 확인");
            } else {
                a.setStatus(ActionStatus.TARGET);
            }
            correctiveActionRepo.save(a);
        }
    }

    private String location(Defect d) {
        StringBuilder sb = new StringBuilder();
        append(sb, "시트", d.getLocationSheet());
        append(sb, "행", d.getLocationRow());
        append(sb, "ID", d.getLocationId());
        return sb.toString();
    }

    private void append(StringBuilder sb, String key, String value) {
        if (value == null || value.isBlank()) return;
        if (sb.length() > 0) sb.append(", ");
        sb.append(key).append(":").append(value);
    }
}
