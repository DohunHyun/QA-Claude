package com.nh.qagpt.service.parser;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 업로드 파일을 형식에 맞는 DocumentParser로 라우팅한다 (spec §4.1).
 * 지원: Excel(.xlsx)·PPTX(.pptx) — Apache POI, HWPX(.hwpx) — OWPML 전용 파서.
 */
@Service
public class DocumentParserRouter {

    private final List<DocumentParser> parsers;

    public DocumentParserRouter(List<DocumentParser> parsers) {
        this.parsers = parsers;
    }

    public ParsedDocument parse(byte[] content, String fileName, String contentType) {
        return parsers.stream()
                .filter(p -> p.supports(fileName, contentType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "지원하지 않는 파일 형식입니다(.xlsx/.pptx/.hwpx 지원): " + fileName))
                .parse(content, fileName, contentType);
    }
}
