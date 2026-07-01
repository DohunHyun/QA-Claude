# QA-GPT (QA-Claude)

AI 기반 IT SI 산출물 **자동 품질검토·개선 플랫폼**. 산출물 업로드 → 품질검토 결과서 + 개선 원본 + 시정조치관리대장 자동 생성.
해커톤 프로젝트 (개발 기간 ~2026.7.15).

## 기술 스택

- **언어/DB**: Java + PostgreSQL
- **프론트**: HTML
- 실증 기준: 실제 프로젝트 「투자자문 관리시스템 구축」(코드 `NBIA`)의 QA 품질점검 케이스 가이드 + 시정조치 PoC 데이터

## 문서 지도

| 경로 | 내용 |
|------|------|
| `docs/spec.md` | 메인 스펙 (제품 정의·결함 판정 체계·체크리스트 정의) |
| `docs/prd.md` | PRD (예정) |
| `docs/checklists/` | 산출물별 검증 기준 체크리스트 12종 |
| `docs/archive/spec_old.md` | 구버전 스펙 보관 |

## 프로젝트 규칙

- **판정 어휘**: `[개선]`(=ERROR, 필수 수정) / `[권고]`(=WARNING, 권장). `[ERROR]`/`[WARNING]` 원어 대신 이 어휘로 통일.
- **문서번호 체계**: `PM-*` (예: 지침서 `PM-141-01`, 테일러링 `PM-120-01`, 추적표 `PM-310-01`, 시정조치서 `PM-342-03`). `PMO-*` 아님.
- **태깅**: 현재 보류.
- `Agents.md`는 사용하지 않음 — 참조 금지.

## 워크플로우

- **git이 원본(source of truth)**, Notion은 미러. 문서 변경은 git에 먼저 반영 후 Notion에 동기화.
- GitHub: https://github.com/DohunHyun/QA-Claude
- 커밋 메시지는 한국어, 판정어휘/문서번호 정합성 유지.

## Agent skills

### Issue tracker

이슈·PRD는 GitHub Issues에 관리 (`gh` CLI 사용). 외부 PR은 트리아지 대상 아님. See `docs/agents/issue-tracker.md`.

### Triage labels

기본 5종 라벨 사용 (`needs-triage`/`needs-info`/`ready-for-agent`/`ready-for-human`/`wontfix`). See `docs/agents/triage-labels.md`.

### Domain docs

단일 컨텍스트 (루트 `CONTEXT.md` + `docs/adr/`). See `docs/agents/domain.md`.
