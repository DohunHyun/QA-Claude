package com.nh.qagpt.service.classifier;

import com.nh.qagpt.domain.enums.ArtifactType;
import com.nh.qagpt.service.ai.ClaudeClient;
import com.nh.qagpt.service.parser.ParsedDocument;
import org.springframework.stereotype.Service;

/**
 * Claude 기반 산출물 유형 자동 인식. 파일명·시트 구조·헤더 컬럼을 근거로 11종에 맵핑한다.
 */
@Service
public class ClaudeDocumentClassifier implements DocumentClassifier {

    private final ClaudeClient claude;

    public ClaudeDocumentClassifier(ClaudeClient claude) {
        this.claude = claude;
    }

    @Override
    public ArtifactType classify(ParsedDocument document) {
        // TODO: claude.complete(...)로 유형 인식 프롬프트 실행 후 ArtifactType 매핑.
        //       매칭 실패 시 ArtifactType.UNKNOWN 반환.
        throw new UnsupportedOperationException("TODO: 산출물 유형 자동 인식 구현");
    }
}
