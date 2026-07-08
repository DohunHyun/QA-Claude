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
 * 패키지·XML 골격은 실제 한컴오피스(한글 9.6)가 생성한 HWPX(PM-141 실샘플)를 실측 대조해 맞췄다:
 * opf:package 직접 루트(hpf:HWPML 래퍼 없음)·전체 네임스페이스 셋·charPr→borderFill/paraPr→tabPr
 * 참조 무결성·첫 문단 secPr(용지)+colPr(단)·Preview/PrvText.txt·container 이중 rootfile.
 * (최종 렌더링 확인은 한글 프로그램 필요 — 구조는 실파일과 동형)
 */
public class HwpxReviewReportWriter {

    private static final String REVIEWER = "AI품질검토봇";

    /** 한컴 실파일과 동일한 네임스페이스 셋 (head/sec 루트 공통). */
    private static final String NS_ALL =
            "xmlns:ha=\"http://www.hancom.co.kr/hwpml/2011/app\" "
            + "xmlns:hp=\"http://www.hancom.co.kr/hwpml/2011/paragraph\" "
            + "xmlns:hp10=\"http://www.hancom.co.kr/hwpml/2016/paragraph\" "
            + "xmlns:hs=\"http://www.hancom.co.kr/hwpml/2011/section\" "
            + "xmlns:hc=\"http://www.hancom.co.kr/hwpml/2011/core\" "
            + "xmlns:hh=\"http://www.hancom.co.kr/hwpml/2011/head\" "
            + "xmlns:hhs=\"http://www.hancom.co.kr/hwpml/2011/history\" "
            + "xmlns:hm=\"http://www.hancom.co.kr/hwpml/2011/master-page\" "
            + "xmlns:hpf=\"http://www.hancom.co.kr/schema/2011/hpf\" "
            + "xmlns:dc=\"http://purl.org/dc/elements/1.1/\" "
            + "xmlns:opf=\"http://www.idpf.org/2007/opf/\" "
            + "xmlns:ooxmlchart=\"http://www.hancom.co.kr/hwpml/2016/ooxmlchart\" "
            + "xmlns:epub=\"http://www.idpf.org/2007/ops\" "
            + "xmlns:config=\"urn:oasis:names:tc:opendocument:xmlns:config:1.0\"";

    public byte[] write(ReviewResult result) {
        List<String> lines = buildLines(result);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(out)) {

            // mimetype: 반드시 첫 엔트리 + 무압축(STORED). 엔트리 구성은 한컴 실파일과 동일.
            stored(zip, "mimetype", "application/hwp+zip".getBytes(StandardCharsets.US_ASCII));
            deflated(zip, "version.xml", versionXml());
            deflated(zip, "Contents/header.xml", headerXml());
            deflated(zip, "Contents/section0.xml", sectionXml(lines));
            deflated(zip, "Preview/PrvText.txt", prvText(lines));
            deflated(zip, "settings.xml", settingsXml());
            deflated(zip, "META-INF/container.xml", containerXml());
            deflated(zip, "Contents/content.hpf", contentHpf());

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

    // ── OWPML 파트 (실측: 한글 9.6 생성 PM-141 실파일과 동형) ──────
    private byte[] versionXml() {
        return utf8("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>"
                + "<hv:HCFVersion xmlns:hv=\"http://www.hancom.co.kr/hwpml/2011/version\" "
                + "tagetApplication=\"WORDPROCESSOR\" major=\"5\" minor=\"0\" micro=\"5\" buildNumber=\"0\" "
                + "os=\"1\" xmlVersion=\"1.1\" application=\"QA-GPT\" appVersion=\"1.0\"/>");
    }

    /** 실측: hpf ns 선언 + content.hpf/PrvText 이중 rootfile. */
    private byte[] containerXml() {
        return utf8("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>"
                + "<ocf:container xmlns:ocf=\"urn:oasis:names:tc:opendocument:xmlns:container\" "
                + "xmlns:hpf=\"http://www.hancom.co.kr/schema/2011/hpf\">"
                + "<ocf:rootfiles>"
                + "<ocf:rootfile full-path=\"Contents/content.hpf\" media-type=\"application/hwpml-package+xml\"/>"
                + "<ocf:rootfile full-path=\"Preview/PrvText.txt\" media-type=\"text/plain\"/>"
                + "</ocf:rootfiles></ocf:container>");
    }

    /** 실측: opf:package 직접 루트(hpf:HWPML 래퍼 없음) + 전체 ns + spine에 header 포함. */
    private byte[] contentHpf() {
        return utf8("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<opf:package " + NS_ALL + " version=\"\" unique-identifier=\"\" id=\"\">"
                + "<opf:metadata>"
                + "<opf:title>단계말 검토결과서</opf:title>"
                + "<opf:language>ko</opf:language>"
                + "<opf:meta name=\"creator\" content=\"text\">" + REVIEWER + "</opf:meta>"
                + "</opf:metadata>"
                + "<opf:manifest>"
                + "<opf:item id=\"header\" href=\"Contents/header.xml\" media-type=\"application/xml\"/>"
                + "<opf:item id=\"section0\" href=\"Contents/section0.xml\" media-type=\"application/xml\"/>"
                + "<opf:item id=\"settings\" href=\"settings.xml\" media-type=\"application/xml\"/>"
                + "</opf:manifest>"
                + "<opf:spine>"
                + "<opf:itemref idref=\"header\" linear=\"yes\"/>"
                + "<opf:itemref idref=\"section0\"/>"
                + "</opf:spine></opf:package>");
    }

    /**
     * 실측 골격: beginNum + refList(fontfaces→borderFills→charProperties→tabProperties→
     * paraProperties→styles). 참조 무결성 — charPr/paraPr의 borderFillIDRef=3, paraPr의 tabPrIDRef=0.
     */
    private byte[] headerXml() {
        // 한컴은 7개 언어 스크립트별 fontface를 선언한다(실측 itemCnt=7).
        String[] langs = {"HANGUL", "LATIN", "HANJA", "JAPANESE", "OTHER", "SYMBOL", "USER"};
        StringBuilder fontfaces = new StringBuilder("<hh:fontfaces itemCnt=\"" + langs.length + "\">");
        for (String lang : langs) {
            fontfaces.append("<hh:fontface lang=\"").append(lang).append("\" fontCnt=\"1\">")
                    .append("<hh:font id=\"0\" face=\"함초롬바탕\" type=\"TTF\" isEmbedded=\"0\">")
                    .append("<hh:typeInfo familyType=\"FCAT_GOTHIC\" weight=\"6\" proportion=\"9\" contrast=\"0\" ")
                    .append("strokeVariation=\"1\" armStyle=\"1\" letterform=\"1\" midline=\"1\" xHeight=\"1\"/>")
                    .append("</hh:font></hh:fontface>");
        }
        fontfaces.append("</hh:fontfaces>");

        // 실측 borderFill 형태 — id 1~3 (charPr/paraPr가 3을 참조, pageBorderFill이 1을 참조).
        StringBuilder borderFills = new StringBuilder("<hh:borderFills itemCnt=\"3\">");
        for (int id = 1; id <= 3; id++) {
            borderFills.append("<hh:borderFill id=\"").append(id)
                    .append("\" threeD=\"0\" shadow=\"0\" centerLine=\"NONE\" breakCellSeparateLine=\"0\">")
                    .append("<hh:slash type=\"NONE\" Crooked=\"0\" isCounter=\"0\"/>")
                    .append("<hh:backSlash type=\"NONE\" Crooked=\"0\" isCounter=\"0\"/>")
                    .append("<hh:leftBorder type=\"NONE\" width=\"0.1 mm\" color=\"#000000\"/>")
                    .append("<hh:rightBorder type=\"NONE\" width=\"0.1 mm\" color=\"#000000\"/>")
                    .append("<hh:topBorder type=\"NONE\" width=\"0.1 mm\" color=\"#000000\"/>")
                    .append("<hh:bottomBorder type=\"NONE\" width=\"0.1 mm\" color=\"#000000\"/>")
                    .append("<hh:diagonal type=\"SOLID\" width=\"0.1 mm\" color=\"#000000\"/>")
                    .append("<hc:fillBrush><hc:winBrush faceColor=\"none\" hatchColor=\"#FF000000\" alpha=\"0\"/></hc:fillBrush>")
                    .append("</hh:borderFill>");
        }
        borderFills.append("</hh:borderFills>");

        String charProperties = "<hh:charProperties itemCnt=\"1\">"
                + "<hh:charPr id=\"0\" height=\"1000\" textColor=\"#000000\" shadeColor=\"none\" "
                + "useFontSpace=\"0\" useKerning=\"0\" symMark=\"NONE\" borderFillIDRef=\"3\">"
                + "<hh:fontRef hangul=\"0\" latin=\"0\" hanja=\"0\" japanese=\"0\" other=\"0\" symbol=\"0\" user=\"0\"/>"
                + "<hh:ratio hangul=\"100\" latin=\"100\" hanja=\"100\" japanese=\"100\" other=\"100\" symbol=\"100\" user=\"100\"/>"
                + "<hh:spacing hangul=\"0\" latin=\"0\" hanja=\"0\" japanese=\"0\" other=\"0\" symbol=\"0\" user=\"0\"/>"
                + "<hh:relSz hangul=\"100\" latin=\"100\" hanja=\"100\" japanese=\"100\" other=\"100\" symbol=\"100\" user=\"100\"/>"
                + "<hh:offset hangul=\"0\" latin=\"0\" hanja=\"0\" japanese=\"0\" other=\"0\" symbol=\"0\" user=\"0\"/>"
                + "</hh:charPr></hh:charProperties>";

        String tabProperties = "<hh:tabProperties itemCnt=\"1\">"
                + "<hh:tabPr id=\"0\" autoTabLeft=\"0\" autoTabRight=\"0\"/></hh:tabProperties>";

        String paraProperties = "<hh:paraProperties itemCnt=\"1\">"
                + "<hh:paraPr id=\"0\" tabPrIDRef=\"0\" condense=\"0\" fontLineHeight=\"0\" snapToGrid=\"1\" "
                + "suppressLineNumbers=\"0\" checked=\"0\">"
                + "<hh:align horizontal=\"JUSTIFY\" vertical=\"BASELINE\"/>"
                + "<hh:heading type=\"NONE\" idRef=\"0\" level=\"0\"/>"
                + "<hh:breakSetting breakLatinWord=\"KEEP_WORD\" breakNonLatinWord=\"KEEP_WORD\" widowOrphan=\"0\" "
                + "keepWithNext=\"0\" keepLines=\"0\" pageBreakBefore=\"0\" lineWrap=\"BREAK\"/>"
                + "<hh:autoSpacing eAsianEng=\"0\" eAsianNum=\"0\"/>"
                + "<hh:margin><hc:intent value=\"0\" unit=\"HWPUNIT\"/><hc:left value=\"0\" unit=\"HWPUNIT\"/>"
                + "<hc:right value=\"0\" unit=\"HWPUNIT\"/><hc:prev value=\"0\" unit=\"HWPUNIT\"/>"
                + "<hc:next value=\"0\" unit=\"HWPUNIT\"/></hh:margin>"
                + "<hh:lineSpacing type=\"PERCENT\" value=\"160\" unit=\"HWPUNIT\"/>"
                + "<hh:border borderFillIDRef=\"3\" offsetLeft=\"0\" offsetRight=\"0\" offsetTop=\"0\" "
                + "offsetBottom=\"0\" connect=\"0\" ignoreMargin=\"0\"/>"
                + "</hh:paraPr></hh:paraProperties>";

        String styles = "<hh:styles itemCnt=\"1\">"
                + "<hh:style id=\"0\" type=\"PARA\" name=\"바탕글\" engName=\"Normal\" paraPrIDRef=\"0\" "
                + "charPrIDRef=\"0\" nextStyleIDRef=\"0\" langID=\"1042\" lockForm=\"0\"/></hh:styles>";

        return utf8("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>"
                + "<hh:head " + NS_ALL + " version=\"1.1\" secCnt=\"1\">"
                + "<hh:beginNum page=\"1\" footnote=\"1\" endnote=\"1\" pic=\"1\" tbl=\"1\" equation=\"1\"/>"
                + "<hh:refList>"
                + fontfaces + borderFills + charProperties + tabProperties + paraProperties + styles
                + "</hh:refList></hh:head>");
    }

    /** 실측: 첫 문단 첫 run에 secPr(A4 용지)+ctrl(colPr 1단) — 한컴이 요구하는 섹션 최소 설정. */
    private byte[] sectionXml(List<String> lines) {
        String secPr = "<hp:secPr id=\"\" textDirection=\"HORIZONTAL\" spaceColumns=\"1130\" tabStop=\"8000\" "
                + "outlineShapeIDRef=\"1\" memoShapeIDRef=\"0\" textVerticalWidthHead=\"0\" masterPageCnt=\"0\">"
                + "<hp:grid lineGrid=\"0\" charGrid=\"0\" wonggojiFormat=\"0\"/>"
                + "<hp:startNum pageStartsOn=\"BOTH\" page=\"0\" pic=\"0\" tbl=\"0\" equation=\"0\"/>"
                + "<hp:visibility hideFirstHeader=\"0\" hideFirstFooter=\"0\" hideFirstMasterPage=\"0\" "
                + "border=\"SHOW_ALL\" fill=\"SHOW_ALL\" hideFirstPageNum=\"0\" hideFirstEmptyLine=\"0\" showLineNumber=\"0\"/>"
                + "<hp:lineNumberShape restartType=\"0\" countBy=\"0\" distance=\"0\" startNumber=\"0\"/>"
                + "<hp:pagePr landscape=\"WIDELY\" width=\"59528\" height=\"84188\" gutterType=\"LEFT_ONLY\">"
                + "<hp:margin header=\"2268\" footer=\"2268\" gutter=\"0\" left=\"5669\" right=\"5669\" top=\"4252\" bottom=\"4252\"/>"
                + "</hp:pagePr>"
                + "<hp:footNotePr><hp:autoNumFormat type=\"DIGIT\" userChar=\"\" prefixChar=\"\" suffixChar=\")\" supscript=\"0\"/>"
                + "<hp:noteLine length=\"-1\" type=\"SOLID\" width=\"0.12 mm\" color=\"#000000\"/>"
                + "<hp:noteSpacing betweenNotes=\"283\" belowLine=\"567\" aboveLine=\"850\"/>"
                + "<hp:numbering type=\"CONTINUOUS\" newNum=\"1\"/>"
                + "<hp:placement place=\"EACH_COLUMN\" beneathText=\"0\"/></hp:footNotePr>"
                + "<hp:endNotePr><hp:autoNumFormat type=\"DIGIT\" userChar=\"\" prefixChar=\"\" suffixChar=\")\" supscript=\"0\"/>"
                + "<hp:noteLine length=\"14692344\" type=\"SOLID\" width=\"0.12 mm\" color=\"#000000\"/>"
                + "<hp:noteSpacing betweenNotes=\"0\" belowLine=\"567\" aboveLine=\"850\"/>"
                + "<hp:numbering type=\"CONTINUOUS\" newNum=\"1\"/>"
                + "<hp:placement place=\"END_OF_DOCUMENT\" beneathText=\"0\"/></hp:endNotePr>"
                + "<hp:pageBorderFill type=\"BOTH\" borderFillIDRef=\"1\" textBorder=\"PAPER\" headerInside=\"0\" "
                + "footerInside=\"0\" fillArea=\"PAPER\"><hp:offset left=\"1417\" right=\"1417\" top=\"1417\" bottom=\"1417\"/></hp:pageBorderFill>"
                + "<hp:pageBorderFill type=\"EVEN\" borderFillIDRef=\"1\" textBorder=\"PAPER\" headerInside=\"0\" "
                + "footerInside=\"0\" fillArea=\"PAPER\"><hp:offset left=\"1417\" right=\"1417\" top=\"1417\" bottom=\"1417\"/></hp:pageBorderFill>"
                + "<hp:pageBorderFill type=\"ODD\" borderFillIDRef=\"1\" textBorder=\"PAPER\" headerInside=\"0\" "
                + "footerInside=\"0\" fillArea=\"PAPER\"><hp:offset left=\"1417\" right=\"1417\" top=\"1417\" bottom=\"1417\"/></hp:pageBorderFill>"
                + "</hp:secPr>";
        String colPr = "<hp:ctrl><hp:colPr id=\"\" type=\"NEWSPAPER\" layout=\"LEFT\" colCount=\"1\" "
                + "sameSz=\"1\" sameGap=\"0\"/></hp:ctrl>";

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>");
        sb.append("<hs:sec ").append(NS_ALL).append(">");
        for (int i = 0; i < lines.size(); i++) {
            sb.append("<hp:p id=\"").append(i)
              .append("\" paraPrIDRef=\"0\" styleIDRef=\"0\" pageBreak=\"0\" columnBreak=\"0\" merged=\"0\">")
              .append("<hp:run charPrIDRef=\"0\">");
            if (i == 0) {
                sb.append(secPr).append(colPr); // 첫 문단에 섹션 설정(실측 패턴)
            }
            sb.append("<hp:t>").append(escape(lines.get(i))).append("</hp:t>")
              .append("</hp:run></hp:p>");
        }
        sb.append("</hs:sec>");
        return utf8(sb.toString());
    }

    private byte[] settingsXml() {
        return utf8("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>"
                + "<ha:HWPApplicationSetting xmlns:ha=\"http://www.hancom.co.kr/hwpml/2011/app\" "
                + "xmlns:config=\"urn:oasis:names:tc:opendocument:xmlns:config:1.0\">"
                + "<ha:CaretPosition listIDRef=\"0\" paraIDRef=\"0\" pos=\"0\"/>"
                + "</ha:HWPApplicationSetting>");
    }

    /** 한컴 미리보기 텍스트(실측: UTF-8 + CRLF). */
    private byte[] prvText(List<String> lines) {
        return String.join("\r\n", lines).getBytes(StandardCharsets.UTF_8);
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
