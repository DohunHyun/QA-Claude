# QA-Claude 프론트엔드 (퍼블리싱) 문서

> 백엔드가 아직 얇은 상태에서 **화면을 먼저 퍼블리싱**한 결과물 정리. 이후 세션이 이 문서를 읽고 이어서 확장할 수 있도록 작성.
> 디자인 기준: 사내 **iQMS 품질관리 포털** 스크린샷 3장 (네이비 상단 헤더 + 회색-블루 좌측 사이드바 + 흰색 테이블 카드).
> 실증 기준 프로젝트: `NBIA` (은행) 투자자문 관리시스템 구축.

## 0. 위치 · 실행

- 모든 정적 파일: `src/main/resources/static/` (Spring Boot 정적 리소스 → 루트로 서빙)
- 로컬 미리보기 (백엔드 없이 목업 동작):
  ```bash
  python3 -m http.server 8088 --bind 127.0.0.1 \
    --directory /Users/kmh/Desktop/QA-Claude/src/main/resources/static
  ```
  접속: `http://127.0.0.1:8088/login.html`
- 실제 백엔드까지 보려면 Spring Boot(8080) 기동 후 그쪽 `/login.html` 접속. `/api/*` 미연결 시 화면은 **자동으로 목업 폴백**.

## 1. 파일 구성

```
static/
├── index.html              # login.html로 리다이렉트 (랜딩)
├── login.html              # 로그인 — 계정 퀵선택 + 아이디/비번 검증
├── dashboard.html          # 나의업무(포털) — 알림·검증현황·공지·ActionItem·결재
├── my-validations.html     # 내 프로젝트 검증(회차 이력)
├── projects.html           # 프로젝트 목록 (역할별 필터)
├── project-new.html        # 프로젝트 신청 (PM) → 신청(REQUESTED), 승인 후 검증 가능
├── project-detail.html     # 프로젝트 현황 — ?projectId= 동적 렌더 + 선택 드롭다운
├── project-approval.html   # ★ 프로젝트 승인 (관리자) — 승인/반려 + 상태변경
├── validation-upload.html  # 산출물 업로드·검증 (실 API /api/validate 연결)
├── validation-progress.html# 검토 진행도 — 프로젝트 선택 + 단계 필터(4-Phase)
├── validation-result.html  # 검증 결과 — 프로젝트+검토회차 선택(?reviewId=)
├── corrective-actions.html # 시정조치관리대장 — 프로젝트 선택(3그룹 17열, §4.4)
├── review-report.html      # 단계말 검토결과서 — 프로젝트+단계 선택(QA 승인)
├── standards.html          # 업무표준 — 체크리스트 12종 + 결함 판정체계
├── statistics.html         # 현황/통계 — 범위(전체/프로젝트) 선택 동적 집계
├── _skeleton.html          # API 연결 스켈레톤 백업 (신버전 — QA승인·교차정합성 포함)
├── checklists/             # 체크리스트 md 12종 (docs/checklists/ 미러 — standards '기준 보기'가 fetch)
└── assets/
    ├── css/app.css         # iQMS 테마 전체 (단색 네이비 헤더 #102a4e)
    └── js/
        ├── layout.js       # 공통 레이아웃 + RBAC + 알림 벨
        ├── session.js      # 계정/세션 (localStorage)
        └── mock.js         # 목업 데이터 + 저장소(프로젝트/상태/알림)
```

## 2. 공통 모듈

### 2.1 `assets/js/layout.js` — 레이아웃 + 권한 엔진
페이지는 `<body data-nav="..." data-page="...">`로 활성 메뉴를 지정하고, 본문은 `<main class="main">`로 감싼다. layout.js가 상단 헤더·좌측 사이드바를 **동적 생성**하고 `.main`을 `.shell` 안으로 이동.

- **NAV 정의**: 상단 대분류 → 좌측 그룹/항목. 각 항목에 `roles: [...]` (미지정=전체 허용).
  - 상단: `portal(나의업무)`, `project(프로젝트)`, `validate(산출물검증)`, `action(시정조치)`, `report(결과물)`, `standard(업무표준)`, `stats(현황/통계)`
- **역할 게이팅**:
  - 상단/사이드바 메뉴: `can(roles)`로 필터링
  - 버튼/요소: HTML에 `data-role="PM,QA"` → 그 역할만 노출. `data-role-mode="disable"` 부가 시 숨김 대신 **회색 비활성**(`.is-disabled`).
  - **접근 가드**: 권한 없는 역할이 URL 직접 진입 시 본문을 "접근 권한 없음"으로 대체(기존 노드는 삭제 않고 `display:none` → 페이지 스크립트 오류 방지). 판정은 **상단 대분류 → 사이드 그룹 → 항목(`data-page` 매칭) 3단계 roles를 모두 통과**해야 허용 (예: `project-approval`은 대분류 project가 전체 허용이어도 항목 roles `관리자`로 차단).
  - **조회전용 배너**: 역할이 `고객사`/`일반`이면 상단에 🔒 배너.
- **알림 벨**: 상단 우측 🔔 + 안읽은 개수 배지. `window.QAGPT_BELL.refresh()`로 갱신.
- 노출 전역: `window.QAGPT_LAYOUT = { role, applyRoleGates, can }`, `window.QAGPT_BELL = { refresh }`.

로드 순서(모든 콘텐츠 페이지): `mock.js` → `session.js` → `layout.js` → 페이지 인라인 스크립트.
> ⚠️ 인라인 스크립트에서 `var M = window.MOCK;` 선언 필수 (미선언 시 `M is not defined`로 조용히 실패 — project-new에서 실제 발생했던 버그).

### 2.2 `assets/js/session.js` — 계정/세션
- `window.QAGPT` (전역명은 QAGPT 유지 — 표시명만 QA-Claude).
- localStorage 키 `qagpt.user`.
- `ACCOUNTS` 레지스트리 → `getAccounts()`, `findAccount(id)`, `authenticate(id, pw)`(검증, 실패 시 null), `nameFromId(id)`.
- `getUser()`(폴백 = PM 김명호), `setUser(u)`, `clearUser()`.

**계정 (비밀번호 모두 `1234`, 시연용):**

| 아이디 | 이름 | 역할 |
|--------|------|------|
| `kmh`  | 김명호 | PM |
| `park` | 박서준 | PM |
| `hdh`  | 현도훈 | QA |
| `khg`  | 김홍규 | 관리자 |
| `client` | 고객담당 | 고객사 |
| `user` | 일반사용자 | 일반 |

### 2.3 `assets/js/mock.js` — 데이터 + 저장소
정적 목업 배열 + localStorage 기반 저장소 3종.

- **정적 데이터**: `ARTIFACTS`(11종 산출물), `PROJECTS`(6건), `REVIEWS`(검증 이력, `project` 코드 보유), `DEFECTS`(시정조치 표본, 각 항목 `project` 코드 보유 — 미지정은 NBIA 기본값), `NOTICES`, `ACTIONS`, `APPROVALS`.
- **유틸**: `esc`, `sevBadge`, `stateHtml`(상태→배지).
- **사용자/필터**:
  - `currentUser()` — QAGPT.getUser() 래핑
  - `visibleProjects()` — **PM=본인 담당(pm===이름)만, 그 외=전체**
  - `visibleReviews()` — visibleProjects에 속한 검증 이력만
  - `validatableProjects()` — visibleProjects 중 **승인(ACTIVE/APPROVED)된 것만** → 산출물 검증 화면의 대상. 승인 전(REQUESTED)·반려(REJECTED)는 제외.
  - `defectsFor(projectCode)` — 해당 프로젝트의 시정조치(결함) 목록 (시정조치관리대장 화면에서 사용)
- **저장소 (localStorage)**:
  - `qagpt.projects` — 사용자가 신청한 신규 프로젝트. `addProject(input)`, `allProjects()`(기본+신규+상태오버라이드 반영)
  - `qagpt.projstatus` — 승인/반려 상태 오버라이드. `setProjectStatus(id, status, approver)`, `pendingProjects()`
  - `qagpt.notifications` — 알림. `notificationsFor(name)`, `unreadCountFor(name)`, `markAllReadFor(name)`
  - `qagpt.reports` — 단계말 검토결과서 발급 기록(`프로젝트id/단계` 키). `issuedReport(pid, stage)`, `issueReport(proj, stage, by)`
  - `setProjectStatus`는 상태 저장 후 **해당 프로젝트 PM에게 알림 자동 생성**(승인=approved / 반려=rejected). `issueReport`도 PM에게 발급 알림 생성(type=report).

**localStorage 키 요약 / 초기화:**
```js
localStorage.removeItem('qagpt.user')          // 로그인 세션
localStorage.removeItem('qagpt.projects')      // 신규 신청 프로젝트
localStorage.removeItem('qagpt.projstatus')    // 승인/반려 상태
localStorage.removeItem('qagpt.notifications')  // 알림
localStorage.removeItem('qagpt.reports')        // 검토결과서 발급 기록
```

### 2.4 `assets/css/app.css` — 테마 · 타이포 정책
- iQMS 테마(네이비 헤더 `#102a4e`) + 공용 컴포넌트: 패널/KPI/테이블(`.tbl`)/배지(`.badge`)/필(`.pill`)/상태(`.state`)/버튼(`.btn`)/**모달**(`.modal-back`/`.modal`).
- **한글 줄바꿈 정책** (글자 단위 세로 꺾임 방지):
  - `body { word-break: keep-all }` — 한글도 공백(단어) 단위로만 줄바꿈. 좁은 셀에서 글자가 한 자씩 세로로 쌓이는 현상 차단.
  - 짧은 라벨류(`.pill`/`.state`/`.btn`/`.badge`/KPI 라벨/`.tbl th·td.num`)는 `white-space: nowrap` — 아예 줄바꿈 금지.
  - `.p-body { overflow-x: auto }` — 표가 패널보다 넓어지면 찌그러지는 대신 **패널 내부 가로 스크롤**. 광폭 표는 기존 `.tbl-scroll` 래퍼와 병행.
  - 긴 설명 컬럼(결함내용 등)은 인라인 `max-width`로 폭을 제한해 단어 단위로 자연 줄바꿈.

## 3. RBAC (역할별 접근) 매핑

스펙 §6.1 5개 사용자 유형 기준.

| 상단 메뉴 | 접근 역할 |
|-----------|-----------|
| 나의업무 / 프로젝트 / 업무표준 / 현황·통계 | 전체 |
| 산출물 검증 | PM, QA |
| 시정조치 / 결과물 | PM, QA, 관리자, 고객사 |

| 사이드/버튼 (예) | 역할 |
|------------------|------|
| 프로젝트 신청(`project-new`), 신청 버튼, 검증 업로드/실행, 재검증 | PM |
| 프로젝트 승인대기(`project-approval`) | 관리자 |
| 단계말 검토결과서 승인/발급/임시저장, 결재 처리 | QA(및 관리자) |
| 고객사·일반 | 조회 전용(편집 버튼 숨김 + 배너) |

## 4. 주요 화면별 동작

- **login** → 계정 클릭 시 자동입력, `authenticate` 검증 후 `setUser` → dashboard.
- **project-new** → 필수값 검증 → `M.addProject`(현재 PM 소유, 상태 REQUESTED)로 저장 → 성공 박스에 **"관리자 승인 후 산출물 검증 가능, 결과는 알림으로 전달"** 안내 + 프로젝트 목록 링크. 실 API `POST /api/projects`도 호출(성공/실패 무관 목업 저장). ※ 신청 직후 산출물 업로드로 넘어가는 자동 이동은 **제거됨**(승인 게이팅으로 대체).
- **project-detail** → 상단 **프로젝트 선택 드롭다운**(visibleProjects) + `?projectId=`로 진입. 선택 프로젝트 기준으로 헤더·**단계 흐름(상태/단계 자동 계산)**·KPI·산출물별 현황·회차별 이력을 모두 `M.REVIEWS`(project 코드 필터)에서 **동적 렌더**. 승인(ACTIVE/APPROVED)만 "산출물 검증" 버튼 노출. `projects.html`의 이름·현황 링크가 `?projectId=` 전달.
- **validation-upload** → 대상 프로젝트 드롭다운은 `M.validatableProjects()`로 **승인(ACTIVE/APPROVED)된 것만** 노출. 승인된 프로젝트가 없으면 ⛔ 안내 + 업로드/검증 **비활성화**. `?projectId=`(승인건 한정) 자동선택, `?stage=`로 산출물 유형 단계 필터. **멀티파일 업로드 지원**(input `multiple` + 드래그앤드롭): 1개 → `POST /api/validate`(유형 수동 지정 가능), 2개 이상 → `POST /api/validate-batch`(`files` 파라미터, 파일별 자동 인식 — 유형 드롭다운 비활성화). 배치 결과는 파일별 판정+결함표로 렌더, 실패 시 목업 결과. 4-Phase 진행 애니메이션.
- **project-approval** (관리자) → 승인 대기 목록에서 **승인**(→ACTIVE) / **반려**(→REJECTED) → `setProjectStatus` → 목록·전체상태 갱신 + PM 알림 생성.
- **dashboard** → 상단 🔔 **알림 패널**(현재 사용자 대상, NEW 표시, "모두 읽음"→벨 배지 제거). **KPI 4종은 visibleProjects/Reviews/defects 기반 동적 집계**(진행중 PJT·검증중 회차·미조치 개선·승인대기). **내 프로젝트 표는 코드 컬럼 대신 담당 PM 이름 표시**(QA/관리자 뷰에서 프로젝트별 PM 식별용). 검증현황/공지/ActionItem/결재. 결재 "처리"는 프로젝트 신청→approval, 그 외→review-report로 라우팅.
- **statistics** → **범위 드롭다운(전체 프로젝트 / 각 프로젝트)**. 선택 범위의 REVIEWS·DEFECTS로 KPI(누적검증·개선·권고·조치완료율)·결함유형 6종 분포·개선/권고 도넛·단계별(관리/분석/설계) 통과율을 **동적 집계**.
- **review-report** → **프로젝트 + 단계(관리/분석/설계) 선택**. 해당 범위 REVIEWS로 검토 개요·종합 판정, DEFECTS(단계 산출물 필터)로 항목별 결과(개선/권고/OK)·AI 개선 산출물 목록 렌더. **QA 승인 버튼**: 개선(ERROR) 합계 0 + 검토이력 존재 시에만 활성 → 클릭 시 confirm → `M.issueReport`(qagpt.reports 저장) → "발급 완료(일자)" 상태로 전환·유지 + PM에게 발급 알림. `?projectId=` 지원.
- **validation-progress** → **프로젝트 선택 드롭다운 + 단계 필터**. 선택 프로젝트의 진행 중(RUNNING) 4-Phase 패널 + 검증 큐/이력(`M.REVIEWS` project 필터). 행 클릭 → `validation-result.html?reviewId=`.
- **validation-result** → **프로젝트 + 검토 회차 2단 드롭다운**. `?reviewId=`(프로젝트 자동 역추적) / `?projectId=` 지원. 선택 회차 기준 헤더·KPI·판정·Phase·결함 상세(`M.DEFECTS` 산출물명 필터)·결과물 동적 렌더. 통과=결함없음, 진행중=안내.
- **corrective-actions** → **프로젝트 선택 드롭다운** + 판정/조치상태/검색 필터. `M.defectsFor(code)`로 §4.4 3그룹 17열 스키마 렌더, 대상/완료/잔여·기준일을 **프로젝트별 집계**. `?projectId=` 지원.
- **standards** → 체크리스트 12종 표(단계 필터, 컬럼: No·단계·문서코드·산출물명·파일·보기 — API 연결 상태 컬럼은 제거됨) + **"기준 보기" 버튼**: `checklists/<file>.md` fetch → 간이 md 렌더러(제목·표·리스트·`[개선]`/`[권고]` 배지)로 **모달**(app.css `.modal-back`/`.modal`, ESC·배경클릭 닫기) 표시. ⚠️ `static/checklists/`는 `docs/checklists/`의 미러 — 원본(docs) 수정 시 복사 필요.

## 5. 백엔드 연동 지점 (실 API vs 목업)

| 화면 | 실 API 호출 | 폴백 |
|------|-------------|------|
| project-new | `POST /api/projects` | 목업 저장 후에도 목록 반영 |
| validation-upload | `POST /api/validate`(1개) / `POST /api/validate-batch`(2개+), `GET /api/projects/{id}` | 목업 결함표 |
| (그 외) | 없음 (목업 데이터) | — |

도메인 enum과 정합 (`src/main/java/.../domain/enums`): `ArtifactType`(11), `Stage`(관리/분석/설계), `Severity`(개선/권고), `DefectType`(6), `Perspective`(2), `ProjectStatus`, `ReviewStatus`, `ActionStatus`.

**세션/알림/승인을 백엔드로 이관할 때:**
- `session.authenticate` → 로그인 API
- `mock.setProjectStatus` → `PUT /api/projects/{id}/status`
- `mock.notificationsFor` 등 → 알림 API
- `mock.visibleProjects/visibleReviews` → 서버측 권한 필터

## 6. 프로젝트 규칙 준수 (CLAUDE.md)

- 판정 어휘 `[개선]`(ERROR)/`[권고]`(WARNING) 사용, 원어 배지는 별칭 병기.
- 문서번호 `PM-*`(예: 시정조치서 `PM-342-03`), 개발산출물 `AN##`/`DS##`.
- 표시 명칭은 **QA-Claude** (내부 식별자 `QAGPT`/`qagpt.*`는 유지).
- AI 생성 텍스트 한자 금지, 기술용어 영문 허용.

## 7. TODO / 확장 후보

- [ ] 단계 전환(관리→분석→설계 진행) 버튼/가드
- [ ] 알림 이벤트 확장: 검증 완료, QA 예외승인 (단계말 결과서 발급 알림은 완료)
- [x] project-detail을 실제 선택 프로젝트(`?projectId=`)에 바인딩 — 완료(드롭다운 + 동적 렌더)
- [x] statistics를 visibleProjects/Reviews 기반 실집계로 전환 — 완료(범위 선택 동적 집계)
- [x] 모든 조회 화면 프로젝트별 지원 — 완료(progress·result·corrective-actions·detail·review-report·statistics)
- [ ] 고객사/일반 전용 축소 대시보드
- [ ] localStorage 초기화 버튼(데모 리셋) UI
- [ ] 백엔드 연동 시 §5 매핑대로 API 교체
```
