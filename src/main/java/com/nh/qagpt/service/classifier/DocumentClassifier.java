package com.nh.qagpt.service.classifier;

import com.nh.qagpt.domain.enums.ArtifactType;
import com.nh.qagpt.service.parser.ParsedDocument;

/** 파싱된 산출물의 유형을 자동 인식한다 (11종 중 하나 또는 UNKNOWN). */
public interface DocumentClassifier {

    ArtifactType classify(ParsedDocument document);
}
