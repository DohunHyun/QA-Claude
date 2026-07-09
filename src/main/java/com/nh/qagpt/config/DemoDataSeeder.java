package com.nh.qagpt.config;

import com.nh.qagpt.domain.Defect;
import com.nh.qagpt.domain.Document;
import com.nh.qagpt.domain.Project;
import com.nh.qagpt.domain.ReviewResult;
import com.nh.qagpt.domain.enums.*;
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
 * 기동 시 데모 데이터(프로젝트 3건 + 검토 회차 + 결함)를 시드한다(빈 DB일 때만).
 * 프론트 목업(assets/js/mock.js)의 PROJECTS/REVIEWS 와 동형으로 맞춰
 * 실 API 화면(검증 현황/검증 결과)이 목업 기반 화면들과 데이터 정합성을 갖도록 한다.
 * (H2 인메모리는 재기동마다 재시드, PostgreSQL은 최초 1회.)
 */
@Component
@Order(1)
public class DemoDataSeeder implements ApplicationRunner {

    private final ProjectRepository projectRepo;
    private final DocumentRepository documentRepo;
    private final ReviewResultRepository reviewRepo;

    public DemoDataSeeder(ProjectRepository projectRepo, DocumentRepository documentRepo,
                          ReviewResultRepository reviewRepo) {
        this.projectRepo = projectRepo;
        this.documentRepo = documentRepo;
        this.reviewRepo = reviewRepo;
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

    @Override
    public void run(ApplicationArguments args) {
        if (projectRepo.count() > 0 || reviewRepo.count() > 0) return;

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
            doc.setFileName(c[0] + "-" + at.getChecklistKey() + "-" + c[1] + "_V" + round + ".0.xlsx");
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
        d.setPerspective(Perspective.ARTIFACT);
        if (sev == Severity.IMPROVEMENT) {
            d.setDefectType(idx == 0 ? DefectType.MISSING_REQUIRED : DefectType.CONTENT_ERROR);
            d.setDescription(at.getLabel() + " 필수 항목 누락/표준 미준수 (검증 엔진 상세 #" + (idx + 1) + ")");
            d.setImprovementGuide("해당 항목을 표준 양식에 맞게 보완");
        } else {
            d.setDefectType(DefectType.ETC);
            d.setDescription(at.getLabel() + " 보완 권고 사항 #" + (idx + 1));
            d.setImprovementGuide("권고 사항 반영 검토");
        }
        return d;
    }
}
