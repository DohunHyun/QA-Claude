package com.nh.qagpt.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Claude API 연동 설정 (application.yml: qagpt.claude.*). */
@ConfigurationProperties(prefix = "qagpt.claude")
public class ClaudeProperties {

    /** Anthropic API base URL. */
    private String baseUrl = "https://api.anthropic.com";
    /** API 키 (환경변수 CLAUDE_API_KEY 주입). */
    private String apiKey = "";
    /** 사용 모델 id. */
    private String model = "claude-sonnet-4-6";
    /** anthropic-version 헤더 값. */
    private String version = "2023-06-01";
    /** 응답 최대 토큰. */
    private int maxTokens = 4096;
    /** 샘플링 온도. 재검증 재현성(spec S2 수락기준)을 위해 기본 0.0 (결정적). */
    private double temperature = 0.0;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    /** API 키가 설정되어 있는지 (미설정 시 LLM 단계는 건너뛰고 규칙검증만 수행). */
    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
