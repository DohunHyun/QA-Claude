# 검토결과서 HWPX 표지 디자인 적용 — 설계

- 작성일: 2026-07-13
- 대상: `HwpxReviewReportWriter.writeAggregate(...)` (검토결과서 **발급** 경로)
- 참조 디자인: `NHEFS-PM-342-02-품질점검보고서(분석설계)_v1.0_20260626.hwpx` (한글로 만든 실파일)

## 배경 / 문제

QA 승인 후 "검토결과서 발급" 버튼을 누르면 `writeAggregate`가 HWPX를 생성한다.
현재 결과물은 표지 없이 "단계말 검토결과서" 제목 문단 + 결함 표만 있어 밋밋하다.
발급물이 실제 품질검토 문서처럼 보이도록, **참조 파일 1페이지(표지) 디자인을 그대로 재사용**하고
기존 검토결과 내용은 2페이지 이후에 참조 문서 양식으로 재스타일한다.

핵심 요구: **비슷하게 다시 그리는 게 아니라, 참조 파일의 실제 OWPML을 템플릿으로 박제**한다.

## 목표 / 비목표

**목표**
- 발급 HWPX 1페이지 = 참조 표지 디자인 그대로(로고·제목·문서정보 표·하단 박스), 텍스트 값만 발급 데이터로 치환.
- 2페이지 이후 = 기존 검토결과 내용을 참조 헤더의 폰트·표 스타일로 재스타일.
- 한글에서 오류 없이 열리는 유효한 HWPX 패키지 유지.

**비목표**
- 개별 검토 다운로드 경로(`write`)는 이번 범위 아님(표지 헬퍼는 공용으로 만들어 추후 적용 여지만 남긴다).
- 참조 파일의 목차/본문 등 2페이지 이후 원문 콘텐츠 재현은 하지 않는다(우리 검토 데이터만 렌더).

## 참조 표지(1페이지) 구성 — 실측

`Contents/section0.xml` 시작 ~ `pageBreak="1"` 문단(개정이력=2페이지 시작) 직전:

| 요소 | 참조 값 | 발급 시 치환 |
|------|---------|--------------|
| 로고 | 농협정보시스템(`BinData/image1.bmp`, `binaryItemIDRef="image1"`) | 그대로 유지 |
| 프로젝트명 | `테스트검증프로젝트 1차` | `project.getName()` |
| 제목 | `품질점검보고서(분석/설계단계)` | `품질검토 결과서({단계그룹}단계)` |
| 부제 | `- 프로젝트관리 -` | `- {project.getCode()} -` (집계 발급이라 특정 산출물명 대신 코드) |
| 문서번호 | `NHEFS-PM-342-02` | `{project.getCode()}-PM-342-02` 자동생성 |
| 버전 | `1.0` | `1.0`(고정 기본값) |
| 제·개정일자 | `2026.06.26` | `approvedDate`(yyyy.MM.dd) |
| 하단 박스 | `품질검토결과서` | 유지 |

- 참조 스타일: 폰트 3종(함초롬바탕·맑은 고딕·굴림체), charPr 43개, borderFill 66개.
  현재 생성기의 미니 헤더(charPr 4개)로는 표지를 못 그리므로 **참조 `header.xml`을 통째 재사용**한다.
- 이미지는 `content.hpf` 매니페스트에 `<opf:item id="image1" href="BinData/image1.bmp" media-type="image/bmp"/>`로 선언된다.
- 참조 파일 metadata의 Fasoo DRM trace 태그는 우리가 패키지를 새로 만들며 자연 제거된다.

## 설계

### 번들 리소스 (`src/main/resources/templates/hwpx/`)
참조 파일에서 추출해 저장:
- `header.xml` — 참조 헤더 전체(표지가 참조하는 모든 폰트·테두리·글자모양 포함).
- `cover.xml` — 표지 문단 조각(첫 `<hp:p>` ~ pageBreak 직전). 6개 값 자리에 안정적 토큰을 두고
  치환한다. 토큰은 참조 원문 값을 그대로 앵커로 사용(예: `<hp:t>테스트검증프로젝트 1차</hp:t>`)하거나
  명시적 placeholder(`{{PROJECT_NAME}}` 등)로 치환해 둔다 — 조각 경계 안에서만 치환하므로 안전.
- `BinData/image1.bmp` — 농협정보시스템 로고.

### 생성 흐름 (`writeAggregate` 재작성)
1. `header.xml` = 번들 참조 헤더 그대로.
2. `section0.xml` =
   `<hs:sec …>` + **표지 조각(값 치환)** + `pageBreak="1"` 문단부터 시작하는 **2페이지 검토결과 본문** + `</hs:sec>`.
   - 표지 조각 첫 문단에 이미 secPr(용지 설정)가 있으므로 단일 섹션 유지. 별도 secPr 추가 금지.
   - 2페이지 본문은 `pageBreak="1"`로 새 페이지에서 시작.
3. 2페이지 본문 문단/표는 **참조 헤더에 존재하는** charPr/paraPr/borderFill ID로 렌더:
   - 본문 텍스트 / 섹션 제목 / 표 헤더(음영) / 표 본문 셀 / 표 테두리 각각에 맞는 참조 ID를 구현 시 선정.
   - 기존 콘텐츠 구성(검증 대상 산출물 / 단계 결과 요약 / 최종 평가 / QA 승인)은 유지.
4. 패키징: mimetype(STORED 첫 엔트리) + version.xml + header.xml + section0.xml +
   `BinData/image1.bmp` + content.hpf(매니페스트에 image1 item 포함) + Preview/PrvText.txt + settings.xml + container.xml.

### 코드 구조
- 표지 로딩·치환·2페이지 렌더는 `HwpxReviewReportWriter` 안에 응집하되,
  표지 조각 로딩+치환은 별도 private 헬퍼(`renderCover(...)`)로 분리해 `write` 경로가 나중에 재사용 가능하게 한다.
- 리소스 로딩은 클래스패스(`getResourceAsStream`)로.

## 검증

- 이 환경엔 한글 프로그램이 없어 **최종 렌더링은 눈으로 확인 불가**.
- 자동 검증: 생성 바이트를 ZIP으로 다시 열어 (a) mimetype 첫 엔트리·STORED, (b) 모든 파트 존재,
  (c) 각 XML well-formed, (d) 표지가 참조하는 charPr/paraPr/borderFill/binaryItem IDRef가 header/manifest에 실재(참조 무결성),
  (e) image1.bmp 바이트 포함.
- 최종 눈 확인은 발급물을 한글에서 열어 사용자가 확인.

## 리스크

- **참조 무결성**: 2페이지 본문이 참조 헤더에 없는 ID를 쓰면 한글이 열 때 깨진다 → 사용 ID를 헤더에서 실재 확인.
- **조각 경계**: 표지 조각이 `hp:p` 중간에서 잘리면 XML 깨짐 → pageBreak 문단 시작 태그 경계에서 정확히 절단, 추출 후 balanced 검증.
- **네임스페이스**: 조각 내 prefix(hp/hc/hh…)가 `<hs:sec>` 루트 ns 선언 하위에서 유효해야 함 → 루트 NS_ALL 유지.
