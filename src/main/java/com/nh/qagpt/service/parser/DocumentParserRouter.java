package com.nh.qagpt.service.parser;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 업로드 파일을 형식에 맞는 DocumentParser로 라우팅한다.
 * 등록된 파서가 없으면 예외. (Excel/PPTX: Apache POI, HWPX: 전용 파서 — 구현 예정)
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
                .orElseThrow(() -> new UnsupportedOperationException(
                        "TODO: 지원 파서 없음 — Excel/PPTX(Apache POI)·HWPX(전용 파서) 구현 필요: " + fileName))
                .parse(content, fileName, contentType);
    }
}
