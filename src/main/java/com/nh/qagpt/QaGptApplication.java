package com.nh.qagpt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * QA-GPT — AI 기반 IT SI 산출물 자동 품질검토·개선 플랫폼.
 * 산출물 업로드 → 4-Phase 검증 → 3종 결과물(개선 산출물·시정조치관리대장·검토결과서) 자동 생성.
 */
@SpringBootApplication
public class QaGptApplication {
    public static void main(String[] args) {
        SpringApplication.run(QaGptApplication.class, args);
    }
}
