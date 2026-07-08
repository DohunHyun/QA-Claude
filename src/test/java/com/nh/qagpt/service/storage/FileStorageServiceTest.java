package com.nh.qagpt.service.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** [P0-B] 원본 파일 저장/로드 — 경로 이탈 방지 포함. */
class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void 저장후_로드하면_동일바이트() {
        FileStorageService storage = new FileStorageService(tempDir.toString());
        byte[] content = "산출물 내용".getBytes();

        String path = storage.store(content, "NHEFS-EA-AN07-배치Job목록_V1.0.xlsx");

        assertThat(storage.exists(path)).isTrue();
        assertThat(storage.load(path)).isEqualTo(content);
        assertThat(path).contains("배치Job목록"); // 한글 파일명 보존
    }

    @Test
    void 파일명_경로문자는_소독된다() {
        FileStorageService storage = new FileStorageService(tempDir.toString());
        String path = storage.store("x".getBytes(), "../../etc/passwd");
        assertThat(path).doesNotContain("..").doesNotContain("/");
        assertThat(storage.load(path)).isEqualTo("x".getBytes());
    }

    @Test
    void 경로이탈_로드는_거부() {
        FileStorageService storage = new FileStorageService(tempDir.toString());
        assertThatThrownBy(() -> storage.load("../outside.txt"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void TODO_또는_빈경로는_없음처리() {
        FileStorageService storage = new FileStorageService(tempDir.toString());
        assertThat(storage.exists("TODO")).isFalse();
        assertThat(storage.exists(null)).isFalse();
        assertThatThrownBy(() -> storage.load("TODO")).isInstanceOf(IllegalStateException.class);
    }
}
