package com.nh.qagpt.service.ai;

/** Claude API 호출 시임. classifier·checklist·generator가 공유한다. */
public interface ClaudeClient {

    /**
     * 단발 메시지 완성 호출. 온도는 설정 기본값(qagpt.claude.temperature).
     * @param systemPrompt 시스템 프롬프트(널 허용)
     * @param userPrompt   사용자 프롬프트
     * @return 모델 응답 텍스트
     */
    String complete(String systemPrompt, String userPrompt);

    /**
     * 온도를 명시하는 완성 호출. 재현성이 필요한 판정 단계는 0.0을 사용한다.
     * @param temperature 0.0(결정적)~1.0
     */
    String complete(String systemPrompt, String userPrompt, double temperature);
}
