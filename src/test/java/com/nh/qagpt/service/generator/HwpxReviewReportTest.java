package com.nh.qagpt.service.generator;

import com.nh.qagpt.domain.Defect;
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
    void OWPML_패키지_구조로_생성된다() throws Exception {
        Map<String, String> entries = readZip(writer.write(resultWithDefect()));
        assertThat(entries).containsKeys(
                "mimetype",
                "version.xml",
                "META-INF/container.xml",
                "META-INF/manifest.xml",
                "Contents/content.hpf",
                "Contents/header.xml",
                "Contents/section0.xml",
                "settings.xml");
        assertThat(entries.get("mimetype")).isEqualTo("application/hwp+zip");
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
}
