package com.nh.qagpt.service.generator;

import com.nh.qagpt.domain.Defect;
import com.nh.qagpt.domain.Document;
import com.nh.qagpt.domain.Project;
import com.nh.qagpt.domain.ReviewResult;
import com.nh.qagpt.domain.enums.ArtifactType;
import com.nh.qagpt.domain.enums.Severity;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * [S7] 단계말 검토결과서를 HWPX(.hwpx = OWPML ZIP 패키지)로 생성 (spec §4.5).
 *
 * 포함: 검증 대상 산출물, 단계 결과 요약, 항목별 결과(개선/권고/OK)+결함유형+관점+근거위치+개선 권고,
 * 최종 평가, QA 서명란. 본문은 Contents/section0.xml 문단으로 기록한다.
 *
 * 주의(정직성): 한글(HWP) 프로그램에서의 실제 렌더링은 수동 검증이 필요하다(빌드 환경에 한글 없음).
 * 본 구현은 OWPML 패키지 구조와 본문 텍스트 포함을 보장하며, 세부 서식 스키마는 후속 정교화 대상이다.
 */
public class HwpxReviewReportWriter {

    private static final String REVIEWER = "AI품질검토봇";

    public byte[] write(ReviewResult result) {
        List<String> lines = buildLines(result);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(out)) {

            // mimetype: 반드시 첫 엔트리 + 무압축(STORED)
            stored(zip, "mimetype", "application/hwp+zip".getBytes(StandardCharsets.US_ASCII));
            deflated(zip, "version.xml", versionXml());
            deflated(zip, "META-INF/container.xml", containerXml());
            deflated(zip, "META-INF/manifest.xml", manifestXml());
            deflated(zip, "Contents/content.hpf", contentHpf());
            deflated(zip, "Contents/header.xml", headerXml());
            deflated(zip, "Contents/section0.xml", sectionXml(lines));
            deflated(zip, "settings.xml", settingsXml());

            zip.finish();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("검토결과서(HWPX) 생성 실패: " + e.getMessage(), e);
        }
    }

    // ── 본문 라인 구성 ─────────────────────────────────────────────
    private List<String> buildLines(ReviewResult result) {
        List<String> lines = new ArrayList<>();
        Project project = result.getProject();
        Document doc = result.getDocument();

        lines.add("단계말 검토결과서");
        lines.add("");
        lines.add("프로젝트: " + safe(project == null ? null : project.getName())
                + " (" + safe(project == null ? null : project.getCode()) + ")");
        lines.add("검증 단계: " + (result.getStage() == null ? "" : result.getStage().getLabel()));
        lines.add("검토자: " + REVIEWER);
        lines.add("");
        lines.add("[검증 대상 산출물]");
        lines.add("- " + artifactLabel(doc) + " / " + safe(doc == null ? null : doc.getFileName()));
        lines.add("");

        List<Defect> defects = result.getDefects();
        long improvements = defects.stream().filter(d -> d.getSeverity() == Severity.IMPROVEMENT).count();
        long recommendations = defects.size() - improvements;
        lines.add("[단계 결과 요약]");
        lines.add("- 총 " + defects.size() + "건 (개선 " + improvements + ", 권고 " + recommendations + ")");
        lines.add("- 최종 결과: " + (result.isPassed() ? "적합(통과)" : "부적합"));
        lines.add("");

        lines.add("[항목별 결과]");
        if (defects.isEmpty()) {
            lines.add("- 부적합 없음 (OK)");
        } else {
            int n = 1;
            for (Defect d : defects) {
                String sev = d.getSeverity() == null ? "" : d.getSeverity().getLabel();
                String type = d.getDefectType() == null ? "" : d.getDefectType().getLabel();
                String persp = d.getPerspective() == null ? "" : d.getPerspective().getLabel();
                lines.add(String.format("%d. [%s] %s · %s · %s", n++, sev, type, persp, location(d)));
                lines.add("   내용: " + safe(d.getDescription()));
                if (d.getImprovementGuide() != null && !d.getImprovementGuide().isBlank()) {
                    lines.add("   개선 권고: " + d.getImprovementGuide());
                }
            }
        }
        lines.add("");

        lines.add("[최종 평가]");
        String verdict = result.isPassed() ? "적합 — 단계 통과"
                : (result.isQaException() ? "부적합 — QA 예외 승인으로 통과 처리" : "부적합 — 개선 필요");
        lines.add("- " + verdict);
        lines.add("");
        lines.add("[QA 승인]");
        lines.add("- QA 검토자: ______________    서명: ______________    승인일: __________");
        lines.add("- QA 승인 여부: " + (result.isQaApproved() ? "승인" : "미승인")
                + (result.isQaException() ? " (예외 승인)" : ""));
        return lines;
    }

    // ── OWPML 파트 ────────────────────────────────────────────────
    private byte[] versionXml() {
        return utf8("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<hv:HCFVersion xmlns:hv=\"http://www.hancom.co.kr/hwpml/2011/version\" "
                + "tagetApplication=\"WORDPROCESSOR\" major=\"5\" minor=\"0\" micro=\"5\" buildNumber=\"0\" "
                + "os=\"1\" xmlVersion=\"1.4\" application=\"QA-GPT\" appVersion=\"1.0\"/>");
    }

    private byte[] containerXml() {
        return utf8("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<ocf:container xmlns:ocf=\"urn:oasis:names:tc:opendocument:xmlns:container\">\n"
                + "  <ocf:rootfiles>\n"
                + "    <ocf:rootfile full-path=\"Contents/content.hpf\" media-type=\"application/hwpml-package+xml\"/>\n"
                + "  </ocf:rootfiles>\n"
                + "</ocf:container>");
    }

    private byte[] manifestXml() {
        return utf8("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<odf:manifest xmlns:odf=\"urn:oasis:names:tc:opendocument:xmlns:manifest:1.0\" version=\"1.2\">\n"
                + "  <odf:file-entry odf:full-path=\"/\" odf:media-type=\"application/hwp+zip\"/>\n"
                + "  <odf:file-entry odf:full-path=\"Contents/content.hpf\" odf:media-type=\"application/hwpml-package+xml\"/>\n"
                + "  <odf:file-entry odf:full-path=\"Contents/header.xml\" odf:media-type=\"application/xml\"/>\n"
                + "  <odf:file-entry odf:full-path=\"Contents/section0.xml\" odf:media-type=\"application/xml\"/>\n"
                + "  <odf:file-entry odf:full-path=\"settings.xml\" odf:media-type=\"application/xml\"/>\n"
                + "</odf:manifest>");
    }

    private byte[] contentHpf() {
        return utf8("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<hpf:HWPML xmlns:hpf=\"http://www.hancom.co.kr/schema/2011/hpf\" "
                + "xmlns:opf=\"http://www.idpf.org/2007/opf/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" version=\"1.4\">\n"
                + "  <opf:package version=\"1.4\" unique-identifier=\"qagpt-review-report\">\n"
                + "    <opf:metadata>\n"
                + "      <opf:title>단계말 검토결과서</opf:title>\n"
                + "      <opf:language>ko</opf:language>\n"
                + "    </opf:metadata>\n"
                + "    <opf:manifest>\n"
                + "      <opf:item id=\"header\" href=\"Contents/header.xml\" media-type=\"application/xml\"/>\n"
                + "      <opf:item id=\"section0\" href=\"Contents/section0.xml\" media-type=\"application/xml\"/>\n"
                + "      <opf:item id=\"settings\" href=\"settings.xml\" media-type=\"application/xml\"/>\n"
                + "    </opf:manifest>\n"
                + "    <opf:spine>\n"
                + "      <opf:itemref idref=\"section0\" linear=\"yes\"/>\n"
                + "    </opf:spine>\n"
                + "  </opf:package>\n"
                + "</hpf:HWPML>");
    }

    private byte[] headerXml() {
        return utf8("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<hh:head xmlns:hh=\"http://www.hancom.co.kr/hwpml/2011/head\" version=\"1.4\" secCnt=\"1\">\n"
                + "  <hh:refList>\n"
                + "    <hh:fontfaces itemCnt=\"1\">\n"
                + "      <hh:fontface lang=\"HANGUL\" fontCnt=\"1\">\n"
                + "        <hh:font id=\"0\" face=\"함초롬바탕\" type=\"TTF\" isEmbedded=\"0\"/>\n"
                + "      </hh:fontface>\n"
                + "    </hh:fontfaces>\n"
                + "    <hh:charProperties itemCnt=\"1\">\n"
                + "      <hh:charPr id=\"0\" height=\"1000\" textColor=\"#000000\" shadeColor=\"none\" useFontSpace=\"0\"/>\n"
                + "    </hh:charProperties>\n"
                + "    <hh:paraProperties itemCnt=\"1\">\n"
                + "      <hh:paraPr id=\"0\" tabPrIDRef=\"0\" condense=\"0\" fontLineHeight=\"0\" snapToGrid=\"1\"/>\n"
                + "    </hh:paraProperties>\n"
                + "    <hh:styles itemCnt=\"1\">\n"
                + "      <hh:style id=\"0\" type=\"PARA\" name=\"바탕글\" engName=\"Normal\" paraPrIDRef=\"0\" charPrIDRef=\"0\"/>\n"
                + "    </hh:styles>\n"
                + "  </hh:refList>\n"
                + "</hh:head>");
    }

    private byte[] sectionXml(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        sb.append("<hs:sec xmlns:hs=\"http://www.hancom.co.kr/hwpml/2011/section\" "
                + "xmlns:hp=\"http://www.hancom.co.kr/hwpml/2011/paragraph\">\n");
        for (String line : lines) {
            sb.append("  <hp:p paraPrIDRef=\"0\" styleIDRef=\"0\">")
              .append("<hp:run charPrIDRef=\"0\"><hp:t>")
              .append(escape(line))
              .append("</hp:t></hp:run></hp:p>\n");
        }
        sb.append("</hs:sec>");
        return utf8(sb.toString());
    }

    private byte[] settingsXml() {
        return utf8("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<ha:HWPApplicationSetting xmlns:ha=\"http://www.hancom.co.kr/hwpml/2011/app\"/>");
    }

    // ── ZIP 헬퍼 ──────────────────────────────────────────────────
    private void stored(ZipOutputStream zip, String name, byte[] data) throws Exception {
        ZipEntry e = new ZipEntry(name);
        e.setMethod(ZipEntry.STORED);
        e.setSize(data.length);
        e.setCompressedSize(data.length);
        CRC32 crc = new CRC32();
        crc.update(data);
        e.setCrc(crc.getValue());
        zip.putNextEntry(e);
        zip.write(data);
        zip.closeEntry();
    }

    private void deflated(ZipOutputStream zip, String name, byte[] data) throws Exception {
        ZipEntry e = new ZipEntry(name);
        e.setMethod(ZipEntry.DEFLATED);
        zip.putNextEntry(e);
        zip.write(data);
        zip.closeEntry();
    }

    // ── 텍스트 헬퍼 ────────────────────────────────────────────────
    private String artifactLabel(Document doc) {
        if (doc == null || doc.getArtifactType() == null || doc.getArtifactType() == ArtifactType.UNKNOWN) {
            return "미인식";
        }
        return doc.getArtifactType().getLabel();
    }

    private String location(Defect d) {
        StringBuilder sb = new StringBuilder();
        appendLoc(sb, "시트", d.getLocationSheet());
        appendLoc(sb, "행", d.getLocationRow());
        appendLoc(sb, "열", d.getLocationColumn());
        appendLoc(sb, "ID", d.getLocationId());
        return sb.length() == 0 ? "근거위치 미상" : sb.toString();
    }

    private void appendLoc(StringBuilder sb, String key, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(", ");
        }
        sb.append(key).append(":").append(value);
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
