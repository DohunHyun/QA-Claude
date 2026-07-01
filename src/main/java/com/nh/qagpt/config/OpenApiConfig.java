package com.nh.qagpt.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger UI (/swagger-ui.html) 문서 메타. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI qaGptOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("QA-GPT API")
                .description("AI 기반 IT SI 산출물 자동 품질검토·개선 플랫폼")
                .version("v0.0.1"));
    }
}
