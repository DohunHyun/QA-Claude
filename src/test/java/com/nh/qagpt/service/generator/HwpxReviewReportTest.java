package com.nh.qagpt.service.generator;

import com.nh.qagpt.domain.Defect;
import com.nh.qagpt.domain.Document;
import com.nh.qagpt.domain.Project;
import com.nh.qagpt.domain.ReviewResult;
import com.nh.qagpt.domain.enums.DefectType;
import com.nh.qagpt.domain.enums.Perspective;
import com.nh.qagpt.domain.enums.Severity;
import com.nh.qagpt.domain.enums.Stage;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/** [S7] 검토결과서(HWPX) — OWPML 패키지 구조 및 본문(항목별 결과·결함유형·관점·근거) 포함 검증. */
class HwpxReviewReportTest {

    private final HwpxReviewReportWriter writer = new HwpxReviewReportWriter();

    private ReviewResult resultWithDefect() {
        ReviewResult result = new ReviewResult();
        result.setStage(Stage.ANALYSIS);
        result.setPassed(false);

        Defect d = new Defect();
        d.setSeverity(Severity.IMPROVEMENT);
        d.setDefectType(DefectType.MISSING_REQUIRED);
        d.setPerspective(Perspective.ARTIFACT);
        d.setLocationSheet("본문");
        d.setLocationColumn("단위업무명");
        d.setDescription("필수 컬럼 누락");
        d.setImprovementGuide("컬럼을 추가하세요.");
        result.addDefect(d);
        return result;
    }

    private Map<String, String> readZip(byte[] bytes) throws Exception {
        Map<String, String> entries = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry e;
            boolean first = true;
            while ((e = zip.getNextEntry()) != null) {
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                byte[] chunk = new byte[4096];
                int r;
                while ((r = zip.read(chunk)) != -1) {
                    buf.write(chunk, 0, r);
                }
                // mimetype은 첫 엔트리 + STORED
                if (first) {
                    assertThat(e.getName()).isEqualTo("mimetype");
                    assertThat(e.getMethod()).isEqualTo(ZipEntry.STORED);
                    first = false;
                }
                entries.put(e.getName(), buf.toString(StandardCharsets.UTF_8));
            }
        }
        return entries;
    }

    @Test
    void OWPML_패키지_구조로_생성된다_한컴실파일_동형() throws Exception {
        Map<String, String> entries = readZip(writer.write(resultWithDefect()));
        // 실측: 한글 9.6 생성 패키지와 동일 엔트리 구성 (manifest.xml 없음, PrvText 있음)
        assertThat(entries).containsKeys(
                "mimetype",
                "version.xml",
                "META-INF/container.xml",
                "Contents/content.hpf",
                "Contents/header.xml",
                "Contents/section0.xml",
                "Preview/PrvText.txt",
                "settings.xml");
        assertThat(entries).doesNotContainKey("META-INF/manifest.xml");
        assertThat(entries.get("mimetype")).isEqualTo("application/hwp+zip");
        // content.hpf 루트는 opf:package 직접 (hpf:HWPML 래퍼 없음 — 실측)
        assertThat(entries.get("Contents/content.hpf")).contains("<opf:package").doesNotContain("<hpf:HWPML");
        // container에 PrvText 이중 rootfile
        assertThat(entries.get("META-INF/container.xml")).contains("Preview/PrvText.txt");
        // 미리보기 텍스트에 본문 포함
        assertThat(entries.get("Preview/PrvText.txt")).contains("단계말 검토결과서");
    }

    @Test
    void 헤더_참조무결성과_섹션설정_한컴규격() throws Exception {
        Map<String, String> entries = readZip(writer.write(resultWithDefect()));
        String header = entries.get("Contents/header.xml");
        String section = entries.get("Contents/section0.xml");
        // charPr/paraPr가 참조하는 borderFill id=3, tabPr id=0이 실제로 선언됨
        assertThat(header).contains("borderFillIDRef=\"3\"").contains("<hh:borderFill id=\"3\"");
        assertThat(header).contains("tabPrIDRef=\"0\"").contains("<hh:tabPr id=\"0\"");
        assertThat(header).contains("secCnt=\"1\"").contains("<hh:beginNum");
        // 첫 문단 첫 run에 용지(secPr)+단(colPr) 설정 (실측 필수 패턴)
        assertThat(section).contains("<hp:secPr").contains("<hp:pagePr").contains("<hp:colPr");
    }

    @Test
    void 본문에_항목별결과_결함유형_관점_근거_포함() throws Exception {
        String section = readZip(writer.write(resultWithDefect())).get("Contents/section0.xml");
        assertThat(section).contains("단계말 검토결과서");
        assertThat(section).contains("항목별 결과");
        assertThat(section).contains("개선");          // 심각도
        assertThat(section).contains("필수항목누락");   // 결함유형
        assertThat(section).contains("산출물");         // 관점
        assertThat(section).contains("시트:본문");      // 근거위치
        assertThat(section).contains("개선 권고");
        assertThat(section).contains("QA 승인");        // 서명란
    }

    @Test
    void 결함없으면_OK로_표기() throws Exception {
        ReviewResult ok = new ReviewResult();
        ok.setStage(Stage.DESIGN);
        ok.setPassed(true);
        String section = readZip(writer.write(ok)).get("Contents/section0.xml");
        assertThat(section).contains("부적합 없음 (OK)");
        assertThat(section).contains("적합 — 단계 통과");
    }

    @Test
    void QA_예외승인_최종평가에_반영() throws Exception {
        ReviewResult r = resultWithDefect();
        r.setQaException(true);
        r.setQaApproved(true);
        String section = readZip(writer.write(r)).get("Contents/section0.xml");
        assertThat(section).contains("QA 예외 승인으로 통과 처리");
    }

    @Test
    void 항목별결과가_한컴표로_렌더된다() throws Exception {
        // 결함 2건 → 표 3행(헤더+2) × 7열 = 21셀
        ReviewResult r = new ReviewResult();
        r.setStage(Stage.ANALYSIS);
        r.setPassed(false);
        for (int i = 0; i < 2; i++) {
            Defect d = new Defect();
            d.setSeverity(Severity.IMPROVEMENT);
            d.setDefectType(DefectType.MISSING_REQUIRED);
            d.setPerspective(Perspective.ARTIFACT);
            d.setLocationSheet("본문");
            d.setDescription("결함" + i);
            r.addDefect(d);
        }
        String section = readZip(writer.write(r)).get("Contents/section0.xml");
        // 표(hp:tbl) + 행/열 수 + 셀 수
        assertThat(section).contains("<hp:tbl ");
        assertThat(section).contains("rowCnt=\"3\"").contains("colCnt=\"7\"");
        assertThat(countOf(section, "<hp:tc ")).isEqualTo(21);
        // 표 헤더 컬럼명
        assertThat(section).contains("결함유형").contains("근거위치").contains("개선 권고");
        // 셀 테두리 참조 borderFill=2 (헤더에 SOLID로 선언)
        assertThat(section).contains("borderFillIDRef=\"2\"");
    }

    @Test
    void 제목이_굵게_가운데정렬_charPr과_paraPr로_선언된다() throws Exception {
        Map<String, String> entries = readZip(writer.write(resultWithDefect()));
        String header = entries.get("Contents/header.xml");
        String section = entries.get("Contents/section0.xml");
        // 제목용 charPr(id=1, bold), 가운데정렬 paraPr(id=1, CENTER)
        assertThat(header).contains("<hh:charPr id=\"1\"").contains("<hh:bold/>");
        assertThat(header).contains("<hh:paraPr id=\"1\"").contains("horizontal=\"CENTER\"");
        // 표 셀용 SOLID 테두리(borderFill id=2)
        assertThat(header).contains("<hh:borderFill id=\"2\"").contains("leftBorder type=\"SOLID\"");
        // 제목 문단이 charPr=1로 렌더 + 첫 문단에 secPr 부착
        assertThat(section).contains("charPrIDRef=\"1\"");
        String firstPara = section.substring(section.indexOf("<hp:p id=\"0\""), section.indexOf("<hp:p id=\"1\""));
        assertThat(firstPara).contains("<hp:secPr").contains("단계말 검토결과서");
    }

    // ── 집계 발급(writeAggregate): 참조 실파일 1페이지 표지 디자인 재사용 ──
    @Test
    void 집계발급_1페이지는_참조표지디자인_2페이지는_검토본문() throws Exception {
        Project project = new Project();
        project.setName("투자자문 관리시스템 구축");
        project.setCode("NBIA");

        ReviewResult r = resultWithDefect();
        Document doc = new Document();
        doc.setFileName("요구사항정의서.xlsx");
        r.setDocument(doc);

        byte[] bytes = writer.writeAggregate(
                project, "분석/설계", java.util.List.of(r), "김QA", java.time.LocalDate.of(2026, 7, 13));
        Map<String, String> entries = readZip(bytes);

        // 표지 로고(농협정보시스템) 포함 + 매니페스트 선언
        assertThat(entries).containsKey("BinData/image1.bmp");
        assertThat(entries.get("Contents/content.hpf"))
                .contains("id=\"image1\"").contains("BinData/image1.bmp");

        // 참조 실파일 헤더 재사용(폰트 일습) — 미니 헤더가 아님
        String header = entries.get("Contents/header.xml");
        assertThat(header).contains("함초롬바탕").contains("맑은 고딕");

        String section = entries.get("Contents/section0.xml");
        // 1페이지 표지: 값만 치환, 디자인(로고 pic·용지설정) 유지
        assertThat(section).contains("품질검토 결과서(분석/설계단계)");
        assertThat(section).contains("투자자문 관리시스템 구축");
        assertThat(section).contains("NBIA-PM-342-02");
        assertThat(section).contains("2026.07.13");
        assertThat(section).contains("binaryItemIDRef=\"image1\"");
        assertThat(section).contains("<hp:secPr").contains("<hp:pic");
        // 참조 원본 표지 값은 남지 않음
        assertThat(section).doesNotContain("테스트검증프로젝트 1차");
        assertThat(section).doesNotContain("NHEFS-PM-342-02");
        assertThat(section).doesNotContain("품질점검보고서(분석/설계단계)");

        // 2페이지: 검토 본문 + 페이지 분리
        assertThat(section).contains("pageBreak=\"1\"");
        assertThat(section).contains("1. 검증 대상 산출물").contains("요구사항정의서.xlsx");
        assertThat(section).contains("4. QA 승인").contains("QA 승인 여부: 승인");

        // section0.xml well-formed
        javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new ByteArrayInputStream(section.getBytes(StandardCharsets.UTF_8)));
    }

    private int countOf(String s, String sub) {
        int c = 0, i = 0;
        while ((i = s.indexOf(sub, i)) >= 0) {
            c++;
            i += sub.length();
        }
        return c;
    }
}
