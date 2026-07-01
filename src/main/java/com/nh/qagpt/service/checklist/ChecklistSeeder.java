package com.nh.qagpt.service.checklist;

import com.nh.qagpt.domain.ChecklistItem;
import com.nh.qagpt.domain.enums.ArtifactType;
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
 * 원본은 docs/checklists/ 이며(git이 유일 기준), Gradle processResources 가 런타임 리소스로 복사한다.
 * 매 기동 시 기존 데이터를 지우고 다시 적재해 docs 편집이 즉시 반영되게 한다(소스 = git).
 * 리소스가 하나도 없으면(복사/빌드 설정 오류) 조용히 통과하지 않고 기동을 실패시킨다.
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
        Resource[] resources = new PathMatchingResourcePatternResolver().getResources(LOCATION);
        if (resources.length == 0) {
            throw new IllegalStateException(
                    "체크리스트 리소스 없음 (" + LOCATION + ") — build.gradle.kts 의 docs/checklists 복사(processResources) 확인 필요");
        }

        List<ChecklistItem> all = new ArrayList<>();
        int skipped = 0;
        for (Resource resource : resources) {
            String baseName = baseName(resource.getFilename());
            if (ArtifactType.fromChecklistKey(baseName) == null) {
                // 산출물 유형에 매핑되지 않는 체크리스트(교차정합성 등)는 조회 경로(findByArtifactType)가 없어 제외.
                // Phase3 교차 검증에서 별도 처리 예정.
                log.info("  {} → 유형 매핑 없음, 시딩 제외", resource.getFilename());
                skipped++;
                continue;
            }
            String markdown = new String(resource.getContentAsByteArray(), StandardCharsets.UTF_8);
            List<ChecklistItem> items = loader.parse(markdown, baseName);
            all.addAll(items);
            log.info("  {} → {}개 항목", resource.getFilename(), items.size());
        }

        // 파싱을 모두 마친 뒤에 교체한다(파싱 실패 시 기존 데이터 보존).
        repository.deleteAllInBatch();
        repository.saveAll(all);
        log.info("체크리스트 시딩 완료 — 파일 {}개(제외 {}), 항목 {}건", resources.length - skipped, skipped, all.size());
    }

    private String baseName(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }
}
