package com.nh.qagpt.service.checklist;

import com.nh.qagpt.domain.ChecklistItem;
import com.nh.qagpt.repository.ChecklistItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 기동 시 {@code classpath:checklists/*.md} 를 파싱해 checklist_item 테이블에 적재한다.
 * 원본은 docs/checklists/ 이며, Gradle processResources 가 런타임 리소스로 복사한다.
 * 이미 적재돼 있으면(count &gt; 0) 건너뛴다(멱등) — 재기동/재검증 시 중복 방지.
 */
@Component
public class ChecklistSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ChecklistSeeder.class);
    private static final String LOCATION = "classpath:checklists/*.md";

    private final ChecklistItemRepository repository;
    private final ChecklistLoader loader;

    public ChecklistSeeder(ChecklistItemRepository repository, ChecklistLoader loader) {
        this.repository = repository;
        this.loader = loader;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        long existing = repository.count();
        if (existing > 0) {
            log.info("체크리스트 이미 적재됨 ({}건) — 시딩 건너뜀", existing);
            return;
        }

        Resource[] resources = new PathMatchingResourcePatternResolver().getResources(LOCATION);
        if (resources.length == 0) {
            log.warn("체크리스트 리소스 없음 ({}) — docs/checklists 복사(Gradle processResources) 확인 필요", LOCATION);
            return;
        }

        List<ChecklistItem> all = new ArrayList<>();
        for (Resource resource : resources) {
            String baseName = baseName(resource.getFilename());
            String markdown = new String(resource.getContentAsByteArray(), StandardCharsets.UTF_8);
            List<ChecklistItem> items = loader.parse(markdown, baseName);
            all.addAll(items);
            log.info("  {} → {}개 항목", resource.getFilename(), items.size());
        }
        repository.saveAll(all);
        log.info("체크리스트 시딩 완료 — 파일 {}개, 항목 {}건", resources.length, all.size());
    }

    private String baseName(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }
}
