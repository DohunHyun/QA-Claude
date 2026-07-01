package com.nh.qagpt.service.parser;

/** 파일 형식별 파서 시임. 구현체: Excel/PPTX(Apache POI), HWPX(전용 파서). */
public interface DocumentParser {

    boolean supports(String fileName, String contentType);

    ParsedDocument parse(byte[] content, String fileName, String contentType);
}
