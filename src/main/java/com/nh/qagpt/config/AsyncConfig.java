package com.nh.qagpt.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 대용량 산출물 검토를 백그라운드로 처리해 업로드 응답 지연을 방지한다 (spec §10.1).
 * ReviewOrchestrator.review() 가 이 executor 위에서 비동기 실행된다.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "reviewExecutor")
    public Executor reviewExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("review-");
        executor.initialize();
        return executor;
    }
}
