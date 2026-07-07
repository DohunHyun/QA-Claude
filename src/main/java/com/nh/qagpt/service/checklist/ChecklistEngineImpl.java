package com.nh.qagpt.service.checklist;

import com.nh.qagpt.domain.Defect;
import com.nh.qagpt.domain.Project;
import com.nh.qagpt.domain.enums.ArtifactType;
import com.nh.qagpt.domain.enums.DefectType;
import com.nh.qagpt.domain.enums.Perspective;
import com.nh.qagpt.domain.enums.Severity;
import com.nh.qagpt.service.parser.ParsedDocument;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 체크리스트 적용 엔진 (spec §5, §7.2).
 *
 * [S1] 배치Job목록 최소 규칙검증만 구현 — 본문 필수컬럼(Job ID·명칭) 존재 여부.
 * TODO(S2+): 4-Phase 전체(표준보증→요구사항품질→목록/정합성) + 룰·LLM 하이브리드로 확장.
 */
@Service
public class ChecklistEngineImpl implements ChecklistEngine {

    /**
     * [S1] 배치Job목록 최소 필수컬럼 (체크리스트 §3 필수 항목 중 핵심).
     * 표기 편차(구 템플릿 "배치Job ID"·신 포맷 "Batch Job ID")를 흡수하도록 "Job ID" 토큰으로 매칭.
     */
    private static final List<String> BATCH_JOB_REQUIRED_COLUMNS = List.of("Job ID", "단위업무명");

    /** [S3] Phase1 표준 보증(파일명·표지·개정이력). 상태 없음 — 직접 인스턴스화(엔진 no-arg 생성자 유지). */
    private final Phase1Validator phase1 = new Phase1Validator();

    @Override
    public List<Defect> apply(ParsedDocument document, ArtifactType type, Project project) {
        List<Defect> defects = new ArrayList<>();
        // [S3] Phase1: 모든 유형 공통 — 프로젝트 등록정보를 기준값으로 파일명/표지/개정이력 검증.
        defects.addAll(phase1.validate(document, project));

        if (type == ArtifactType.BATCH_JOB_LIST) {
            defects.addAll(checkRequiredColumns(document, BATCH_JOB_REQUIRED_COLUMNS));
        }
        // TODO(S4+): 유형별 Phase2~4 체크리스트 적용 확장
        return defects;
    }

    /** 본문 시트의 헤더에서 필수 컬럼 존재 여부를 확인하고, 누락 컬럼을 결함으로 만든다. */
    private List<Defect> checkRequiredColumns(ParsedDocument document, List<String> required) {
        List<Defect> defects = new ArrayList<>();

        Map.Entry<String, List<List<String>>> body = selectBodySheet(document);
        String sheetName = body == null ? "(시트 없음)" : body.getKey();
        List<String> header = body == null ? List.of() : findHeaderRow(body.getValue());
        Set<String> headerNorm = header.stream().map(this::normalize).collect(Collectors.toSet());

        for (String col : required) {
            String target = normalize(col);
            boolean present = headerNorm.stream().anyMatch(h -> h.equals(target) || h.contains(target));
            if (!present) {
                Defect d = new Defect();
                d.setSeverity(Severity.IMPROVEMENT);
                d.setDefectType(DefectType.MISSING_REQUIRED);
                d.setPerspective(Perspective.ARTIFACT);
                d.setChecklistItemKey("batch_job_list.required_column");
                d.setLocationSheet(sheetName);
                d.setLocationColumn(col);
                d.setDescription("필수 컬럼 누락: '" + col + "' (배치Job목록 본문)");
                d.setImprovementGuide("본문 시트에 '" + col + "' 컬럼을 추가하고 값을 기재하세요.");
                defects.add(d);
            }
        }
        return defects;
    }

    /** 본문 시트 선택: 이름에 '본문'/'내용' 포함 우선, 없으면 최대 컬럼 수 시트. */
    private Map.Entry<String, List<List<String>>> selectBodySheet(ParsedDocument document) {
        Map.Entry<String, List<List<String>>> byName = null;
        Map.Entry<String, List<List<String>>> byWidth = null;
        int maxWidth = -1;
        for (Map.Entry<String, List<List<String>>> e : document.getSheets().entrySet()) {
            String name = e.getKey() == null ? "" : e.getKey();
            if (name.contains("본문") || name.contains("내용")) {
                byName = e;
            }
            int width = e.getValue().stream().mapToInt(List::size).max().orElse(0);
            if (width > maxWidth) {
                maxWidth = width;
                byWidth = e;
            }
        }
        return byName != null ? byName : byWidth;
    }

    /** 헤더 행 = 앞쪽 행들 중 비어있지 않은 셀이 가장 많은 행. */
    private List<String> findHeaderRow(List<List<String>> rows) {
        List<String> header = List.of();
        int maxNonEmpty = 0;
        int limit = Math.min(rows.size(), 10);
        for (int i = 0; i < limit; i++) {
            List<String> row = rows.get(i);
            int nonEmpty = (int) row.stream().filter(c -> c != null && !c.isBlank()).count();
            if (nonEmpty > maxNonEmpty) {
                maxNonEmpty = nonEmpty;
                header = row;
            }
        }
        return header;
    }

    private String normalize(String s) {
        return s == null ? "" : s.replaceAll("\\s+", "").toLowerCase();
    }
}
