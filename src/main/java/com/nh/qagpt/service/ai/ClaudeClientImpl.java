package com.nh.qagpt.service.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nh.qagpt.config.ClaudeProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Anthropic Messages API(/v1/messages) 연동. x-api-key 헤더로 인증한다.
 * 모델·버전·max_tokens는 ClaudeProperties(application.yml: qagpt.claude.*)에서 주입.
 */
@Service
public class ClaudeClientImpl implements ClaudeClient {

    private final WebClient claudeWebClient;
    private final ClaudeProperties props;

    public ClaudeClientImpl(@Qualifier("claudeWebClient") WebClient claudeWebClient,
                            ClaudeProperties props) {
        this.claudeWebClient = claudeWebClient;
        this.props = props;
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        return complete(systemPrompt, userPrompt, props.getTemperature());
    }

    @Override
    public String complete(String systemPrompt, String userPrompt, double temperature) {
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            throw new IllegalStateException("CLAUDE_API_KEY 미설정 — 환경변수를 설정하세요.");
        }

        Map<String, Object> body = new HashMap<>();
        body.put("model", props.getModel());
        body.put("max_tokens", props.getMaxTokens());
        body.put("temperature", temperature);
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            body.put("system", systemPrompt);
        }
        body.put("messages", List.of(Map.of("role", "user", "content", userPrompt)));

        MessageResponse response = claudeWebClient.post()
                .uri("/v1/messages")
                .header("x-api-key", props.getApiKey())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(MessageResponse.class)
                .block();

        if (response == null || response.content() == null || response.content().isEmpty()) {
            return "";
        }
        return response.content().get(0).text();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record MessageResponse(List<ContentBlock> content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ContentBlock(String type, String text) {}
}
