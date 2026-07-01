package com.nh.qagpt.service.parser;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 파서가 산출물 원본에서 추출한 정규화 표현. 후속 단계(classifier/checklist)는 이 모델만 본다.
 * - sheets: 표 형태(Excel 시트 등) — 시트명 → 행 목록(각 행은 셀 문자열 리스트).
 * - rawText: 비정형 본문(PPTX/HWPX 텍스트 등).
 */
@Getter
@Setter
@NoArgsConstructor
public class ParsedDocument {

    private String fileName;
    private String contentType;
    private Map<String, List<List<String>>> sheets = new LinkedHashMap<>();
    private String rawText;
}
