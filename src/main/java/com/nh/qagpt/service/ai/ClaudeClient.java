package com.nh.qagpt.service.ai;

/** Claude API 호출 시임. classifier·checklist·generator가 공유한다. */
public interface ClaudeClient {

    /**
     * 단발 메시지 완성 호출.
     * @param systemPrompt 시스템 프롬프트(널 허용)
     * @param userPrompt   사용자 프롬프트
     * @return 모델 응답 텍스트
     */
    String complete(String systemPrompt, String userPrompt);
}
