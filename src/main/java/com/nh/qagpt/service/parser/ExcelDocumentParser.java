package com.nh.qagpt.service.parser;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

/** Excel(.xlsx) 파서 (Apache POI). 각 시트를 문자열 행 목록으로 정규화한다. */
@Component
public class ExcelDocumentParser implements DocumentParser {

    @Override
    public boolean supports(String fileName, String contentType) {
        if (fileName != null && fileName.toLowerCase().endsWith(".xlsx")) {
            return true;
        }
        return contentType != null && contentType.contains("spreadsheetml");
    }

    @Override
    public ParsedDocument parse(byte[] content, String fileName, String contentType) {
        ParsedDocument doc = new ParsedDocument();
        doc.setFileName(fileName);
        doc.setContentType(contentType);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            DataFormatter fmt = new DataFormatter();
            for (int s = 0; s < wb.getNumberOfSheets(); s++) {
                Sheet sheet = wb.getSheetAt(s);
                List<List<String>> rows = new ArrayList<>();
                for (Row row : sheet) {
                    List<String> cells = new ArrayList<>();
                    short last = row.getLastCellNum();
                    for (int c = 0; c < last; c++) {
                        Cell cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                        cells.add(cell == null ? "" : fmt.formatCellValue(cell).trim());
                    }
                    rows.add(cells);
                }
                doc.getSheets().put(sheet.getSheetName(), rows);
            }
        } catch (Exception e) {
            // S1 수락 기준: 파싱 실패 시 사용자에게 오류 메시지 (→ 400)
            throw new IllegalArgumentException("Excel 파싱 실패: " + fileName + " — " + e.getMessage(), e);
        }
        return doc;
    }
}
