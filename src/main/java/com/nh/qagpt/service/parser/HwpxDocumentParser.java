package com.nh.qagpt.service.parser;

import org.springframework.stereotype.Component;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * HWPX(.hwpx) 파서 — 문서작성지침서(PM-141-01) 등 한글 문서 (spec §4.1).
 *
 * HWPX = OWPML ZIP 패키지. Contents/section*.xml의 텍스트 노드(<hp:t>)를 문단 단위로 추출한다.
 * 섹션 XML이 없거나 비어있으면 Preview/PrvText.txt(한컴 미리보기 텍스트)를 폴백으로 사용한다.
 * 표 구조 복원(셀 경계)은 후속 — 현재는 rawText 중심(LLM 판정·Phase1 검증에 충분).
 */
@Component
public class HwpxDocumentParser implements DocumentParser {

    @Override
    public boolean supports(String fileName, String contentType) {
        if (fileName != null && fileName.toLowerCase().endsWith(".hwpx")) {
            return true;
        }
        return contentType != null && contentType.contains("hwp+zip");
    }

    @Override
    public ParsedDocument parse(byte[] content, String fileName, String contentType) {
        ParsedDocument doc = new ParsedDocument();
        doc.setFileName(fileName);
        doc.setContentType(contentType);

        try {
            Map<String, byte[]> sections = new TreeMap<>(); // section0, section1 … 순서 보존
            byte[] preview = null;

            try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(content))) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    String name = entry.getName();
                    if (name.startsWith("Contents/section") && name.endsWith(".xml")) {
                        sections.put(name, readAll(zip));
                    } else if (name.equals("Preview/PrvText.txt")) {
                        preview = readAll(zip);
                    }
                }
            }

            StringBuilder raw = new StringBuilder();
            for (byte[] xml : sections.values()) {
                extractText(xml, raw);
            }
            if (raw.length() == 0 && preview != null) {
                raw.append(new String(preview, StandardCharsets.UTF_8));
            }
            if (raw.length() == 0) {
                throw new IllegalArgumentException("본문 텍스트를 찾지 못했습니다(Contents/section*.xml 없음)");
            }
            doc.setRawText(raw.toString());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("HWPX 파싱 실패: " + fileName + " — " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IllegalArgumentException("HWPX 파싱 실패: " + fileName + " — " + e.getMessage(), e);
        }
        return doc;
    }

    /** StAX로 로컬명 t(문자 런)·p(문단) 요소를 순회하며 텍스트를 문단 단위로 모은다. 네임스페이스 접두사 무관. */
    private void extractText(byte[] xml, StringBuilder out) throws Exception {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        // XXE 방지
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);

        XMLStreamReader reader = factory.createXMLStreamReader(new ByteArrayInputStream(xml));
        boolean inText = false;
        StringBuilder para = new StringBuilder();
        try {
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    String local = reader.getLocalName();
                    if ("t".equals(local)) {
                        inText = true;
                    }
                } else if (event == XMLStreamConstants.CHARACTERS && inText) {
                    para.append(reader.getText());
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    String local = reader.getLocalName();
                    if ("t".equals(local)) {
                        inText = false;
                    } else if ("p".equals(local)) { // 문단 종료 → 줄바꿈
                        if (para.length() > 0) {
                            out.append(para).append('\n');
                            para.setLength(0);
                        }
                    }
                }
            }
            if (para.length() > 0) {
                out.append(para).append('\n');
            }
        } finally {
            reader.close();
        }
    }

    private byte[] readAll(ZipInputStream zip) throws Exception {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int r;
        while ((r = zip.read(chunk)) != -1) {
            buf.write(chunk, 0, r);
        }
        return buf.toByteArray();
    }
}
