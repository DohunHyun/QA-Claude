package com.nh.qagpt.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 정적 리소스(HTML/JS/CSS)를 항상 재검증(no-cache)하도록 캐시 정책을 지정한다.
 *
 * <p>캐시버스팅(?v=) 없이 배포하면 브라우저가 옛 mock.js/화면 스크립트를 그대로 캐시해,
 * 프론트 수정이 배포돼도 사용자 화면엔 반영되지 않는 문제가 있었다(예: 검증중 대기행이
 * 안 없어지는 stale JS). no-cache + ETag 재검증으로 매 요청 최신본을 보장하되,
 * 변경 없으면 304로 저비용 처리한다.
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.noCache());
    }
}
