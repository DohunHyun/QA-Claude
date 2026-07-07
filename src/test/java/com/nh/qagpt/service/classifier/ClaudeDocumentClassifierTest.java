package com.nh.qagpt.service.classifier;

import com.nh.qagpt.config.ClaudeProperties;
import com.nh.qagpt.domain.enums.ArtifactType;
import com.nh.qagpt.service.ai.ClaudeClient;
import com.nh.qagpt.service.parser.ParsedDocument;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [S2] 유형 자동 인식 — 파일명 코드/라벨 기반 결정적 매핑 검증(오프라인, LLM 미호출).
 * API 키 미설정 상태이므로 Claude 폴백은 타지 않는다(재현 가능).
 */
class ClaudeDocumentClassifierTest {

    /** 호출되면 테스트 실패시키는 스텁 — 규칙 경로에서 LLM을 부르지 않음을 보장. */
    private final ClaudeClient noCallClient = new ClaudeClient() {
        @Override
        public String complete(String s, String u) {
            throw new AssertionError("LLM을 호출하면 안 됨(규칙/키없음 경로)");
        }

        @Override
        public String complete(String s, String u, double t) {
            throw new AssertionError("LLM을 호출하면 안 됨(규칙/키없음 경로)");
        }
    };

    private final ClaudeProperties noKeyProps = new ClaudeProperties(); // apiKey="" → hasApiKey()=false
    private final ClaudeDocumentClassifier classifier = new ClaudeDocumentClassifier(noCallClient, noKeyProps);

    private ArtifactType classify(String fileName) {
        ParsedDocument doc = new ParsedDocument();
        doc.setFileName(fileName);
        return classifier.classify(doc);
    }

    @Test
    void 실제샘플_파일명코드로_유형인식() {
        assertThat(classify("NHEFS-EA-AN07-배치Job목록_V1.01.xlsx")).isEqualTo(ArtifactType.BATCH_JOB_LIST);
        assertThat(classify("NHXXX-단위업무코드-AN07-배치Job목록_V1.0_20170601.xlsx")).isEqualTo(ArtifactType.BATCH_JOB_LIST);
        assertThat(classify("NHEFS-EA-AN06-요구사항정의서_V1.01.xlsx")).isEqualTo(ArtifactType.REQUIREMENT_DEFINITION);
        assertThat(classify("NHEFS-EA-AN08-인터페이스정의서_V1.01.xlsx")).isEqualTo(ArtifactType.INTERFACE_DEFINITION);
        assertThat(classify("NHEFS-EA-DS09-배치설계서_V1.0.xlsx")).isEqualTo(ArtifactType.BATCH_DESIGN);
        assertThat(classify("NHEFS-EA-DS10-인터페이스설계서_V1.01.xlsx")).isEqualTo(ArtifactType.INTERFACE_DESIGN);
        assertThat(classify("NHEFS-EA-DS07-프로그램목록_V1.01.xlsx")).isEqualTo(ArtifactType.PROGRAM_LIST);
    }

    @Test
    void PM코드_하이픈유무_모두인식() {
        assertThat(classify("NHEFS-PM-120-01-테일러링결과서_V1.0.xlsx")).isEqualTo(ArtifactType.TAILORING_RESULT);
        assertThat(classify("NHEFS-PM-141-01-문서작성지침서_V1.0.hwpx")).isEqualTo(ArtifactType.DOCUMENT_WRITING_GUIDELINE);
        assertThat(classify("NHEFS-PM-310-01-요구사항추적표_V1.0.xlsx")).isEqualTo(ArtifactType.REQUIREMENT_TRACEABILITY_MATRIX);
    }

    @Test
    void 코드없으면_한글라벨로_인식() {
        assertThat(classify("프로그램목록.xlsx")).isEqualTo(ArtifactType.PROGRAM_LIST);
        assertThat(classify("UI목록_초안.xlsx")).isEqualTo(ArtifactType.UI_LIST);
    }

    @Test
    void 재현성_동일입력_동일결과() {
        String f = "NHEFS-EA-AN07-배치Job목록_V1.01.xlsx";
        assertThat(classify(f)).isEqualTo(classify(f)).isEqualTo(ArtifactType.BATCH_JOB_LIST);
    }

    @Test
    void 코드도라벨도없으면_UNKNOWN() {
        assertThat(classify("무관한첨부.txt")).isEqualTo(ArtifactType.UNKNOWN);
    }
}
