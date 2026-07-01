package com.nh.qagpt.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Claude API 호출용 WebClient. 인증 헤더(x-api-key)는 요청 시 주입한다
 * (ClaudeClientImpl 참조). anthropic-version·content-type만 기본값으로 설정.
 */
@Configuration
@EnableConfigurationProperties(ClaudeProperties.class)
public class WebClientConfig {

    @Bean(name = "claudeWebClient")
    public WebClient claudeWebClient(ClaudeProperties props) {
        return WebClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeader("anthropic-version", props.getVersion())
                .defaultHeader("content-type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
