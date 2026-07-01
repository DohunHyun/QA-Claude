# 백엔드 스캐폴딩 안내

Spring Boot 3.3.5 + Java 17 + JPA(H2/PostgreSQL) + WebClient(Claude) + Apache POI 골격.
비즈니스 로직은 대부분 `TODO`(미구현 시임)이며, 유형·배선·판정 체계·엔티티만 확정돼 있다.

## 실행

```bash
# ⚠️ JDK 17 로 실행할 것. (이 PC의 gradle 기본 JDK가 26이면 Kotlin DSL 평가 시 크래시함)
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

./gradlew bootRun          # 기본 H2 인메모리로 즉시 실행
./gradlew test             # 컨텍스트 로딩 스모크 테스트
```

- 헬스체크: `GET /api/health`
- API 문서: `/swagger-ui.html`
- H2 콘솔: `/h2-console` (jdbc:h2:mem:qagpt)
- 운영(PostgreSQL): `./gradlew bootRun --args='--spring.profiles.active=postgres'` + `CLAUDE_API_KEY`·`DB_*` 환경변수

## 구조 (spec §10.3)

```
com.nh.qagpt
├── config        AsyncConfig · WebClientConfig · ClaudeProperties · OpenApiConfig
├── controller    Project · DocumentUpload · Review · Status
├── domain        Project · Document · ReviewResult · Defect · CorrectiveAction · ChecklistItem (+ enums)
├── repository    JpaRepository 4종
├── service
│   ├── parser        DocumentParser / Router / ParsedDocument   ← Apache POI·HWPX (TODO)
│   ├── classifier    DocumentClassifier (Claude)                ← 유형 인식 (TODO)
│   ├── checklist     ChecklistEngine  ★검증 정확도 핵심 시임    ← 4-Phase 판정 (TODO)
│   ├── ai            ClaudeClient     Anthropic Messages API 연동 (구현됨)
│   └── generator     ResultGenerator  ★결과물 품질 핵심 시임    ← 3종 결과물 (TODO)
├── dto · exception
└── ReviewOrchestrator  4-Phase 흐름 조립 (TODO)
```

## 판정 체계 (코드로 확정)

- **Severity**: 개선(ERROR, 통과불가) / 권고(WARNING, 통과가능)
- **DefectType** 6종: 표준미준수 · 필수항목누락 · 내용오류·불명확 · 미제출 · 중복 · 기타
- **Perspective** 2종: 산출물 / 프로세스
- **Stage / ArtifactType**: 관리·분석·설계 / 11종 산출물(체크리스트 키 매핑)

## 다음 작업 (담당별)

- 파서(Apache POI/HWPX) → `service/parser`
- 유형 인식 → `service/classifier`
- **체크리스트 4-Phase 엔진** → `service/checklist` (PoC 36건 회귀 코퍼스로 검증)
- **3종 결과물 생성** → `service/generator` (3시트/17열, 텍스트 서식 강제)
- 오케스트레이션 → `service/ReviewOrchestrator`
