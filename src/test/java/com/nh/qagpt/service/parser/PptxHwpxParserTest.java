package com.nh.qagpt.service.parser;

import com.nh.qagpt.domain.ReviewResult;
import com.nh.qagpt.domain.enums.Stage;
import com.nh.qagpt.service.generator.HwpxReviewReportWriter;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.junit.jupiter.api.Test;

import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** [P0-A] PPTX·HWPX 파서 — 11종 전체 파싱 가능화 (spec §4.1). */
class PptxHwpxParserTest {

    private final PptxDocumentParser pptxParser = new PptxDocumentParser();
    private final HwpxDocumentParser hwpxParser = new HwpxDocumentParser();

    // ── PPTX ─────────────────────────────────────────────────────
    private byte[] samplePptx() throws Exception {
        try (XMLSlideShow ppt = new XMLSlideShow(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSLFSlide slide = ppt.createSlide();
            XSLFTextBox box = slide.createTextBox();
            box.setAnchor(new Rectangle(50, 50, 400, 100));
            box.setText("프로세스정의서 — 계좌개설 프로세스");

            XSLFTable table = slide.createTable(2, 2);
            table.setAnchor(new Rectangle(50, 200, 400, 100));
            table.getCell(0, 0).setText("액티비티 ID");
            table.getCell(0, 1).setText("액티비티명");
            table.getCell(1, 0).setText("AV-EA-AP-0001");
            table.getCell(1, 1).setText("실명확인");

            ppt.write(out);
            return out.toByteArray();
        }
    }

    @Test
    void PPTX_텍스트와_표를_추출한다() throws Exception {
        ParsedDocument doc = pptxParser.parse(samplePptx(), "NHEFS-EA-AN04-프로세스정의서_V1.0.pptx", null);

        assertThat(doc.getRawText()).contains("슬라이드 1").contains("계좌개설 프로세스");
        assertThat(doc.getSheets()).containsKey("슬라이드1-표1");
        assertThat(doc.getSheets().get("슬라이드1-표1").get(0)).containsExactly("액티비티 ID", "액티비티명");
        assertThat(doc.getSheets().get("슬라이드1-표1").get(1)).containsExactly("AV-EA-AP-0001", "실명확인");
    }

    @Test
    void PPTX_supports_확장자와_MIME() {
        assertThat(pptxParser.supports("a.pptx", null)).isTrue();
        assertThat(pptxParser.supports("a.PPTX", null)).isTrue();
        assertThat(pptxParser.supports(null, "application/vnd.openxmlformats-officedocument.presentationml.presentation")).isTrue();
        assertThat(pptxParser.supports("a.xlsx", null)).isFalse();
    }

    @Test
    void PPTX_깨진파일은_400계열_예외() {
        assertThatThrownBy(() -> pptxParser.parse("not pptx".getBytes(), "b.pptx", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PPTX 파싱 실패");
    }

    // ── HWPX ─────────────────────────────────────────────────────
    /** 자사 HWPX writer 출력물을 그대로 파서에 되돌리는 라운드트립 — OWPML 구조 상호 검증. */
    @Test
    void HWPX_라운드트립_본문텍스트_추출() {
        ReviewResult result = new ReviewResult();
        result.setStage(Stage.ANALYSIS);
        result.setPassed(true);
        byte[] hwpx = new HwpxReviewReportWriter().write(result);

        ParsedDocument doc = hwpxParser.parse(hwpx, "검토결과서.hwpx", null);

        assertThat(doc.getRawText()).contains("단계말 검토결과서");
        assertThat(doc.getRawText()).contains("적합 — 단계 통과");
        assertThat(doc.getRawText()).contains("QA 승인");
    }

    @Test
    void HWPX_supports_확장자와_MIME() {
        assertThat(hwpxParser.supports("지침서.hwpx", null)).isTrue();
        assertThat(hwpxParser.supports(null, "application/hwp+zip")).isTrue();
        assertThat(hwpxParser.supports("a.xlsx", null)).isFalse();
    }

    @Test
    void HWPX_깨진파일은_400계열_예외() {
        assertThatThrownBy(() -> hwpxParser.parse("broken".getBytes(), "b.hwpx", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HWPX 파싱 실패");
    }
}
