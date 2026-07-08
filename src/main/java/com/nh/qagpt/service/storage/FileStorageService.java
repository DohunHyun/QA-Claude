package com.nh.qagpt.service.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * 업로드 산출물 원본 저장/로드 (spec §10.2). 로컬 디렉토리(qagpt.storage.dir, 기본 uploads/)에 보관한다.
 * 원본 바이트가 있어야 비동기 재검증·개선 산출물 생성(원본 포맷 유지)이 재업로드 없이 가능하다.
 */
@Service
public class FileStorageService {

    private final Path baseDir;

    public FileStorageService(@Value("${qagpt.storage.dir:uploads}") String dir) {
        this.baseDir = Path.of(dir);
    }

    /** 바이트를 저장하고 저장 경로(상대)를 반환한다. 파일명 충돌 방지를 위해 UUID 접두사를 붙인다. */
    public String store(byte[] content, String originalFileName) {
        try {
            Files.createDirectories(baseDir);
            String safeName = sanitize(originalFileName);
            String stored = UUID.randomUUID().toString().substring(0, 8) + "_" + safeName;
            Path target = baseDir.resolve(stored);
            Files.write(target, content);
            return stored;
        } catch (IOException e) {
            throw new IllegalStateException("파일 저장 실패: " + originalFileName + " — " + e.getMessage(), e);
        }
    }

    /** 저장 경로로 원본 바이트를 로드한다. */
    public byte[] load(String storagePath) {
        if (storagePath == null || storagePath.isBlank() || "TODO".equals(storagePath)) {
            throw new IllegalStateException("저장된 원본이 없습니다: " + storagePath);
        }
        try {
            Path target = baseDir.resolve(storagePath).normalize();
            if (!target.startsWith(baseDir.normalize())) { // 경로 이탈 방지
                throw new IllegalStateException("잘못된 저장 경로: " + storagePath);
            }
            return Files.readAllBytes(target);
        } catch (IOException e) {
            throw new IllegalStateException("파일 로드 실패: " + storagePath + " — " + e.getMessage(), e);
        }
    }

    public boolean exists(String storagePath) {
        if (storagePath == null || storagePath.isBlank() || "TODO".equals(storagePath)) {
            return false;
        }
        return Files.exists(baseDir.resolve(storagePath));
    }

    /** 경로 구분자·상위 이동 제거(zip slip/traversal 방지). */
    private String sanitize(String name) {
        if (name == null || name.isBlank()) {
            return "artifact";
        }
        String base = name.replace('\\', '/');
        base = base.substring(base.lastIndexOf('/') + 1);
        return base.replaceAll("[^0-9A-Za-z가-힣._()\\-\\[\\]]", "_");
    }
}
