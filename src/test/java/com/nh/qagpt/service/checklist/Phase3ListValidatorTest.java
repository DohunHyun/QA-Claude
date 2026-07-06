package com.nh.qagpt.service.checklist;

import com.nh.qagpt.domain.Defect;
import com.nh.qagpt.domain.enums.ArtifactType;
import com.nh.qagpt.domain.enums.DefectType;
import com.nh.qagpt.service.parser.ParsedDocument;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** [S8] Phase3 목록/정합성 — 유형 확대 필수컬럼·ID중복·개발구분 enum. */
class Phase3ListValidatorTest {

    private final Phase3ListValidator validator = new Phase3ListValidator();

    private ParsedDocument doc(List<List<String>> rows) {
        ParsedDocument d = new ParsedDocument();
        d.getSheets().put("양식", rows);
        return d;
    }

    private boolean has(List<Defect> defects, DefectType type, String descPart) {
        return defects.stream().anyMatch(x -> x.getDefectType() == type
                && x.getDescription() != null && x.getDescription().contains(descPart));
    }

    @Test
    void 프로그램목록_정상이면_결함없음() {
        List<Defect> defects = validator.validate(doc(List.of(
                List.of("단위업무명", "프로그램 ID", "프로그램 명"),
                List.of("AP", "EFDS0I0", "THE QUICKER"),
                List.of("AP", "EFDS0I1", "NH BOX"))), ArtifactType.PROGRAM_LIST);
        assertThat(defects).isEmpty();
    }

    @Test
    void 프로그램목록_필수컬럼_누락검출() {
        List<Defect> defects = validator.validate(doc(List.of(
                List.of("단위업무명", "프로그램 명"))), // 프로그램 ID 누락
                ArtifactType.PROGRAM_LIST);
        assertThat(has(defects, DefectType.MISSING_REQUIRED, "프로그램 ID")).isTrue();
    }

    @Test
    void 인터페이스정의서_필수컬럼_인식() {
        List<Defect> defects = validator.validate(doc(List.of(
                List.of("단위업무명", "인터페이스ID", "인터페이스명"))), ArtifactType.INTERFACE_DEFINITION);
        assertThat(defects).isEmpty();
    }

    @Test
    void ID중복_검출() {
        List<Defect> defects = validator.validate(doc(List.of(
                List.of("단위업무명", "프로그램 ID", "프로그램 명"),
                List.of("AP", "DUP01", "a"),
                List.of("AP", "DUP01", "b"))), ArtifactType.PROGRAM_LIST);
        assertThat(has(defects, DefectType.DUPLICATE, "DUP01")).isTrue();
    }

    @Test
    void 배치_개발구분_비허용값_검출() {
        List<Defect> defects = validator.validate(doc(List.of(
                List.of("단위업무명", "배치Job ID", "개발구분"),
                List.of("여신", "BJ-DE-0001", "유지"),
                List.of("여신", "BJ-DE-0002", "이상값"))), ArtifactType.BATCH_JOB_LIST);
        assertThat(has(defects, DefectType.STANDARD_VIOLATION, "이상값")).isTrue();
    }

    @Test
    void 배치_개발구분_허용값만이면_결함없음() {
        List<Defect> defects = validator.validate(doc(List.of(
                List.of("단위업무명", "배치Job ID", "개발구분"),
                List.of("여신", "BJ-DE-0001", "신규"),
                List.of("여신", "BJ-DE-0002", "삭제"))), ArtifactType.BATCH_JOB_LIST);
        assertThat(defects).isEmpty();
    }
}
