package com.nh.qagpt.service.parser;

import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTableCell;
import org.apache.poi.xslf.usermodel.XSLFTableRow;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * PPTX(.pptx) 파서 (Apache POI XSLF) — 프로세스정의서(AN04)·UI레이아웃(DS03) 등 (spec §4.1).
 * 슬라이드 텍스트는 rawText로, 슬라이드 내 표는 sheets("슬라이드N-표M")로 정규화한다.
 */
@Component
public class PptxDocumentParser implements DocumentParser {

    @Override
    public boolean supports(String fileName, String contentType) {
        if (fileName != null && fileName.toLowerCase().endsWith(".pptx")) {
            return true;
        }
        return contentType != null && contentType.contains("presentationml");
    }

    @Override
    public ParsedDocument parse(byte[] content, String fileName, String contentType) {
        ParsedDocument doc = new ParsedDocument();
        doc.setFileName(fileName);
        doc.setContentType(contentType);

        try (XMLSlideShow ppt = new XMLSlideShow(new ByteArrayInputStream(content))) {
            StringBuilder raw = new StringBuilder();
            int slideNo = 0;
            for (XSLFSlide slide : ppt.getSlides()) {
                slideNo++;
                raw.append("## 슬라이드 ").append(slideNo).append('\n');
                int tableNo = 0;
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTable table) {
                        tableNo++;
                        List<List<String>> rows = new ArrayList<>();
                        for (XSLFTableRow row : table.getRows()) {
                            List<String> cells = new ArrayList<>();
                            for (XSLFTableCell cell : row.getCells()) {
                                String t = cell.getText();
                                cells.add(t == null ? "" : t.trim());
                            }
                            rows.add(cells);
                        }
                        doc.getSheets().put("슬라이드" + slideNo + "-표" + tableNo, rows);
                    } else if (shape instanceof XSLFTextShape text) {
                        String t = text.getText();
                        if (t != null && !t.isBlank()) {
                            raw.append(t.trim()).append('\n');
                        }
                    }
                }
            }
            doc.setRawText(raw.toString());
        } catch (Exception e) {
            // 파싱 실패 시 사용자 메시지 (→ 400)
            throw new IllegalArgumentException("PPTX 파싱 실패: " + fileName + " — " + e.getMessage(), e);
        }
        return doc;
    }
}
