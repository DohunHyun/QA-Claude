/* ============================================================
   QA-Claude 퍼블리싱용 목업 데이터
   (실 백엔드 연동 전까지 화면 시연용. 실증 기준: NBIA 투자자문 PJT)
   ============================================================ */
window.MOCK = (function () {

  var ARTIFACTS = [
    { code:'PM-141-01', stage:'관리', name:'문서작성지침서', type:'DOCUMENT_WRITING_GUIDELINE', wired:false },
    { code:'PM-120-01', stage:'관리', name:'테일러링결과서', type:'TAILORING_RESULT', wired:false },
    { code:'PM-310-01', stage:'관리', name:'요구사항추적표', type:'REQUIREMENT_TRACEABILITY_MATRIX', wired:false },
    { code:'AN06', stage:'분석', name:'요구사항정의서', type:'REQUIREMENT_DEFINITION', wired:false },
    { code:'AN04', stage:'분석', name:'프로세스정의서', type:'PROCESS_DEFINITION', wired:false },
    { code:'AN08', stage:'분석', name:'인터페이스정의서', type:'INTERFACE_DEFINITION', wired:false },
    { code:'AN07', stage:'분석', name:'배치Job목록', type:'BATCH_JOB_LIST', wired:true },
    { code:'DS01', stage:'설계', name:'UI목록', type:'UI_LIST', wired:false },
    { code:'DS07', stage:'설계', name:'프로그램목록', type:'PROGRAM_LIST', wired:false },
    { code:'DS10', stage:'설계', name:'인터페이스설계서', type:'INTERFACE_DESIGN', wired:false },
    { code:'DS09', stage:'설계', name:'배치설계서', type:'BATCH_DESIGN', wired:false }
  ];

  // 프로젝트 목록 — 실 시연용 3건 (모두 김명호 소유, 관리·분석 완료 기준)
  //   P1 NHOB : 설계 진행중 → 시정조치관리대장 시연
  //   P2 NHUL : 설계 진행중(회차 다수) → 검증 현황/회차별 진행상황 시연
  //   P3 NHGBS: 설계 완료 → 단계말 검토결과서 샘플
  var PROJECTS = [
    { id:2001, code:'NHOB', name:'(은행) 국외지점 전산시스템 개선', pm:'김명호', status:'ACTIVE',
      stage:'설계', progress:66, start:'2025-11-03', end:'2026-08-31' },
    { id:2002, code:'NHUL', name:'(은행) U2L 전환 구축', pm:'김명호', status:'ACTIVE',
      stage:'설계', progress:70, start:'2026-01-05', end:'2026-09-30' },
    { id:2003, code:'NHGBS', name:'(은행) GBS 여신시스템 구축', pm:'김명호', status:'ACTIVE',
      stage:'설계', progress:100, start:'2025-09-01', end:'2026-06-30' }
  ];

  // 검증(회차) 이력 — 3개 프로젝트, 단계별(관리·분석·설계)
  var REVIEWS = [
    // ══ NHOB (국외지점 전산시스템 개선) — 관리✓ 분석✓ 설계 진행중 ══
    // 관리단계 (완료·통과)
    { id:5011, project:'NHOB', artifact:'문서작성지침서', code:'PM-141-01', stage:'관리', round:1, status:'COMPLETED',
      passed:true, error:0, warn:1, date:'2026-03-10' },
    { id:5012, project:'NHOB', artifact:'테일러링결과서', code:'PM-120-01', stage:'관리', round:1, status:'COMPLETED',
      passed:true, error:0, warn:0, date:'2026-03-12' },
    { id:5013, project:'NHOB', artifact:'요구사항추적표', code:'PM-310-01', stage:'관리', round:2, status:'COMPLETED',
      passed:true, error:0, warn:1, date:'2026-03-20' },
    // 분석단계 (완료·통과)
    { id:5021, project:'NHOB', artifact:'요구사항정의서', code:'AN06', stage:'분석', round:2, status:'COMPLETED',
      passed:true, error:0, warn:2, date:'2026-05-02' },
    { id:5022, project:'NHOB', artifact:'배치Job목록', code:'AN07', stage:'분석', round:1, status:'COMPLETED',
      passed:true, error:0, warn:1, date:'2026-05-05' },
    { id:5023, project:'NHOB', artifact:'프로세스정의서', code:'AN04', stage:'분석', round:1, status:'COMPLETED',
      passed:true, error:0, warn:1, date:'2026-05-06' },
    // 설계단계 (진행중 · 결함 → 시정조치)
    { id:5031, project:'NHOB', artifact:'UI목록', code:'DS01', stage:'설계', round:1, status:'COMPLETED',
      passed:true, error:0, warn:0, date:'2026-07-08' },
    { id:5032, project:'NHOB', artifact:'배치설계서', code:'DS09', stage:'설계', round:2, status:'COMPLETED',
      passed:false, error:3, warn:2, date:'2026-07-06' },
    { id:5033, project:'NHOB', artifact:'인터페이스설계서', code:'DS10', stage:'설계', round:1, status:'COMPLETED',
      passed:false, error:2, warn:1, date:'2026-07-06' },
    { id:5034, project:'NHOB', artifact:'프로그램목록', code:'DS07', stage:'설계', round:1, status:'COMPLETED',
      passed:false, error:1, warn:1, date:'2026-07-05' },

    // ══ NHUL (U2L 전환 구축) — 관리✓ 분석✓ 설계 진행중 (회차 다수) ══
    { id:5111, project:'NHUL', artifact:'문서작성지침서', code:'PM-141-01', stage:'관리', round:1, status:'COMPLETED',
      passed:true, error:0, warn:0, date:'2026-02-15' },
    { id:5112, project:'NHUL', artifact:'테일러링결과서', code:'PM-120-01', stage:'관리', round:2, status:'COMPLETED',
      passed:true, error:0, warn:1, date:'2026-02-25' },
    // 분석단계 — 요구사항정의서 회차 진행(1→2→3회차 통과)
    { id:5121, project:'NHUL', artifact:'요구사항정의서', code:'AN06', stage:'분석', round:1, status:'COMPLETED',
      passed:false, error:5, warn:3, date:'2026-04-10' },
    { id:5122, project:'NHUL', artifact:'요구사항정의서', code:'AN06', stage:'분석', round:2, status:'COMPLETED',
      passed:false, error:2, warn:2, date:'2026-04-24' },
    { id:5123, project:'NHUL', artifact:'요구사항정의서', code:'AN06', stage:'분석', round:3, status:'COMPLETED',
      passed:true, error:0, warn:1, date:'2026-05-08' },
    { id:5124, project:'NHUL', artifact:'인터페이스정의서', code:'AN08', stage:'분석', round:1, status:'COMPLETED',
      passed:true, error:0, warn:1, date:'2026-05-10' },
    // 설계단계 — 진행중
    { id:5131, project:'NHUL', artifact:'배치설계서', code:'DS09', stage:'설계', round:1, status:'COMPLETED',
      passed:false, error:4, warn:2, date:'2026-07-02' },
    { id:5132, project:'NHUL', artifact:'배치설계서', code:'DS09', stage:'설계', round:2, status:'COMPLETED',
      passed:true, error:0, warn:0, date:'2026-07-08' },
    { id:5133, project:'NHUL', artifact:'UI목록', code:'DS01', stage:'설계', round:1, status:'COMPLETED',
      passed:false, error:2, warn:3, date:'2026-07-04' },

    // ══ NHGBS (GBS 여신시스템 구축) — 관리✓ 분석✓ 설계✓ (검토결과서 샘플) ══
    { id:5211, project:'NHGBS', artifact:'문서작성지침서', code:'PM-141-01', stage:'관리', round:1, status:'COMPLETED',
      passed:true, error:0, warn:0, date:'2026-01-10' },
    { id:5212, project:'NHGBS', artifact:'테일러링결과서', code:'PM-120-01', stage:'관리', round:1, status:'COMPLETED',
      passed:true, error:0, warn:1, date:'2026-01-12' },
    { id:5213, project:'NHGBS', artifact:'요구사항추적표', code:'PM-310-01', stage:'관리', round:1, status:'COMPLETED',
      passed:true, error:0, warn:0, date:'2026-01-20' },
    { id:5221, project:'NHGBS', artifact:'요구사항정의서', code:'AN06', stage:'분석', round:2, status:'COMPLETED',
      passed:true, error:0, warn:1, date:'2026-03-05' },
    { id:5222, project:'NHGBS', artifact:'프로세스정의서', code:'AN04', stage:'분석', round:1, status:'COMPLETED',
      passed:true, error:0, warn:0, date:'2026-03-08' },
    { id:5223, project:'NHGBS', artifact:'배치Job목록', code:'AN07', stage:'분석', round:1, status:'COMPLETED',
      passed:true, error:0, warn:1, date:'2026-03-10' },
    { id:5231, project:'NHGBS', artifact:'UI목록', code:'DS01', stage:'설계', round:1, status:'COMPLETED',
      passed:true, error:0, warn:1, date:'2026-05-20' },
    { id:5232, project:'NHGBS', artifact:'프로그램목록', code:'DS07', stage:'설계', round:2, status:'COMPLETED',
      passed:true, error:0, warn:0, date:'2026-05-25' },
    { id:5233, project:'NHGBS', artifact:'인터페이스설계서', code:'DS10', stage:'설계', round:1, status:'COMPLETED',
      passed:true, error:0, warn:1, date:'2026-06-02' },
    { id:5234, project:'NHGBS', artifact:'배치설계서', code:'DS09', stage:'설계', round:1, status:'COMPLETED',
      passed:true, error:0, warn:1, date:'2026-06-05' }
  ];

  // 시정조치관리대장 — 각 결함에 stage(단계)·round(회차) 부여 (단계·회차 필터용)
  var DEFECTS = [
    // ══ NHOB (국외지점 전산시스템 개선) ══
    // 관리·분석단계 (완료·조치완료 이력)
    { no:'CA_M01', project:'NHOB', biz:'국외지점', gubun:'관리', stage:'관리', round:1, date:'2026-03-08', reviewer:'AI품질검토봇',
      artifact:'문서작성지침서', file:'NHOB-PM-141-01_문서작성지침서_V1.0a_20260305.hwpx',
      loc:'표지', sev:'개선', dtype:'표준미준수', persp:'산출물',
      desc:'제·개정일자가 프로젝트 관리단계 착수일(2025-11-03) 이전으로 기재됨',
      owner:'김명호', due:'2026-03-12', doneDate:'2026-03-11', status:'DONE', plan:'표지 개정일자 정정',
      checker:'QA', checkDate:'2026-03-12' },
    { no:'CA_A01', project:'NHOB', biz:'국외지점', gubun:'개발', stage:'분석', round:1, date:'2026-04-20', reviewer:'AI품질검토봇',
      artifact:'요구사항정의서', file:'NHOB-DV-AN06-요구사항정의서_V1.0a_20260410.xlsx',
      loc:'시트:요구사항, Row 45 / ID REQ-032', sev:'개선', dtype:'중복', persp:'산출물',
      desc:'요구사항 ID REQ-032 가 2개 행에 중복 정의됨', owner:'김명호', due:'2026-04-24',
      doneDate:'2026-04-23', status:'DONE', plan:'중복 행 통합/재채번', checker:'QA', checkDate:'2026-04-24' },
    { no:'CA_A02', project:'NHOB', biz:'국외지점', gubun:'개발', stage:'분석', round:1, date:'2026-04-20', reviewer:'AI품질검토봇',
      artifact:'요구사항정의서', file:'NHOB-DV-AN06-요구사항정의서_V1.0a_20260410.xlsx',
      loc:'시트:요구사항, Row 60', sev:'권고', dtype:'기타', persp:'산출물',
      desc:'요구사항 근거(출처) 컬럼이 공란 — 추적성 확인 곤란', owner:'김명호', due:'2026-04-26',
      doneDate:'2026-04-25', status:'DONE', plan:'출처(회의록/RFP) 보완', checker:'QA', checkDate:'2026-04-26' },
    // 설계단계 (진행중 · 미조치)
    { no:'CA_D01', project:'NHOB', biz:'국외지점', gubun:'개발', stage:'설계', round:2, date:'2026-07-06', reviewer:'AI품질검토봇',
      artifact:'배치설계서', file:'NHOB-DV-DS09-배치설계서_V1.0a_20260701.xlsx',
      loc:'시트:설계, Row 3~62', sev:'개선', dtype:'필수항목누락', persp:'산출물',
      desc:'배치 Job 다수에 "실패 시 처리방안" 미정의', owner:'김명호', due:'2026-07-10',
      doneDate:'', status:'TARGET', plan:'실패처리(재시도/알림) 컬럼 작성', checker:'', checkDate:'' },
    { no:'CA_D02', project:'NHOB', biz:'국외지점', gubun:'개발', stage:'설계', round:2, date:'2026-07-06', reviewer:'AI품질검토봇',
      artifact:'배치설계서', file:'NHOB-DV-DS09-배치설계서_V1.0a_20260701.xlsx',
      loc:'시트:설계, Row 22', sev:'개선', dtype:'내용오류·불명확', persp:'산출물',
      desc:'배치 수행주기(스케줄) 표기가 일부 Job에서 상호 모순', owner:'김명호', due:'2026-07-10',
      doneDate:'', status:'IN_PROGRESS', plan:'스케줄 정의 재작성', checker:'', checkDate:'' },
    { no:'CA_D03', project:'NHOB', biz:'국외지점', gubun:'개발', stage:'설계', round:2, date:'2026-07-06', reviewer:'AI품질검토봇',
      artifact:'배치설계서', file:'NHOB-DV-DS09-배치설계서_V1.0a_20260701.xlsx',
      loc:'시트:설계, Row 40', sev:'권고', dtype:'기타', persp:'산출물',
      desc:'배치 소요시간(예상)이 일부 Job에만 기재 — 상세화 부족', owner:'김명호', due:'2026-07-11',
      doneDate:'', status:'TARGET', plan:'전 Job 소요시간 산정 권고', checker:'', checkDate:'' },
    { no:'CA_D04', project:'NHOB', biz:'국외지점', gubun:'개발', stage:'설계', round:1, date:'2026-07-06', reviewer:'AI품질검토봇',
      artifact:'인터페이스설계서', file:'NHOB-DV-DS10-인터페이스설계서_V1.0a_20260701.xlsx',
      loc:'문서번호/파일명', sev:'개선', dtype:'표준미준수', persp:'산출물',
      desc:'파일명 날짜(YYYYMMDD) 누락 · 버전표기 소문자 v1.0 (표준 V1.0a)', owner:'김명호',
      due:'2026-07-10', doneDate:'', status:'IN_PROGRESS', plan:'명명규칙 준수 재저장', checker:'', checkDate:'' },
    { no:'CA_D05', project:'NHOB', biz:'국외지점', gubun:'개발', stage:'설계', round:1, date:'2026-07-06', reviewer:'AI품질검토봇',
      artifact:'인터페이스설계서', file:'NHOB-DV-DS10-인터페이스설계서_V1.0a_20260701.xlsx',
      loc:'시트:I/F, Row 18', sev:'권고', dtype:'기타', persp:'산출물',
      desc:'인터페이스 오류코드 정의가 일부 미기재', owner:'김명호', due:'2026-07-11',
      doneDate:'', status:'TARGET', plan:'오류코드 표 보완 권고', checker:'', checkDate:'' },
    { no:'CA_D06', project:'NHOB', biz:'국외지점', gubun:'개발', stage:'설계', round:1, date:'2026-07-05', reviewer:'AI품질검토봇',
      artifact:'프로그램목록', file:'NHOB-DV-DS07-프로그램목록_V1.0a_20260630.xlsx',
      loc:'시트:목록, Row 8 / Level2명', sev:'개선', dtype:'내용오류·불명확', persp:'프로세스',
      desc:'Level2명 오타 "국외지점" → "국회지점" — UI목록·메뉴구조로 연쇄 전염 우려', owner:'김명호',
      due:'2026-07-09', doneDate:'', status:'TARGET', plan:'연쇄 산출물 동시 정정', checker:'', checkDate:'' },

    // ══ NHUL (U2L 전환 구축) ══
    { no:'CA_A11', project:'NHUL', biz:'차세대전환', gubun:'개발', stage:'분석', round:1, date:'2026-04-10', reviewer:'AI품질검토봇',
      artifact:'요구사항정의서', file:'NHUL-DV-AN06-요구사항정의서_V1.0a_20260405.xlsx',
      loc:'시트:요구사항, Row 22 / ID', sev:'개선', dtype:'표준미준수', persp:'산출물',
      desc:'요구사항 ID 체계가 표준(REQ-###)과 불일치', owner:'김명호', due:'2026-04-24',
      doneDate:'2026-04-22', status:'DONE', plan:'ID 재채번', checker:'QA', checkDate:'2026-04-24' },
    { no:'CA_A12', project:'NHUL', biz:'차세대전환', gubun:'개발', stage:'분석', round:2, date:'2026-04-24', reviewer:'AI품질검토봇',
      artifact:'요구사항정의서', file:'NHUL-DV-AN06-요구사항정의서_V2.0a_20260420.xlsx',
      loc:'시트:요구사항, Row 30', sev:'개선', dtype:'중복', persp:'산출물',
      desc:'통합/분리 요구사항 2건이 동일 ID로 중복 기재', owner:'김명호', due:'2026-05-02',
      doneDate:'2026-04-30', status:'DONE', plan:'ID 분리·재채번', checker:'QA', checkDate:'2026-05-02' },
    { no:'CA_D11', project:'NHUL', biz:'차세대전환', gubun:'개발', stage:'설계', round:1, date:'2026-07-02', reviewer:'AI품질검토봇',
      artifact:'배치설계서', file:'NHUL-DV-DS09-배치설계서_V1.0a_20260628.xlsx',
      loc:'시트:설계, Row 15', sev:'개선', dtype:'필수항목누락', persp:'산출물',
      desc:'U2L 전환 대상 배치의 이관 전/후 매핑 정보 미기재', owner:'김명호', due:'2026-07-09',
      doneDate:'', status:'IN_PROGRESS', plan:'전환 매핑표 보완', checker:'', checkDate:'' },
    { no:'CA_D12', project:'NHUL', biz:'차세대전환', gubun:'개발', stage:'설계', round:1, date:'2026-07-04', reviewer:'AI품질검토봇',
      artifact:'UI목록', file:'NHUL-DV-DS01-UI목록_V1.0a_20260701.xlsx',
      loc:'시트:목록, Row 12', sev:'개선', dtype:'내용오류·불명확', persp:'프로세스',
      desc:'화면 ID 명명규칙이 구(U) 체계와 신(L) 체계 혼용', owner:'김명호', due:'2026-07-10',
      doneDate:'', status:'TARGET', plan:'신 체계로 통일', checker:'', checkDate:'' },

    // ══ NHGBS (GBS 여신시스템 구축) — 통과 위주, 권고만 (조치완료) ══
    { no:'CA_R01', project:'NHGBS', biz:'여신관리', gubun:'개발', stage:'분석', round:2, date:'2026-03-05', reviewer:'AI품질검토봇',
      artifact:'요구사항정의서', file:'NHGBS-DV-AN06-요구사항정의서_V2.0a_20260301.xlsx',
      loc:'시트:요구사항, Row 51', sev:'권고', dtype:'기타', persp:'산출물',
      desc:'요구사항 우선순위 컬럼 일부 공란', owner:'김명호', due:'2026-03-12',
      doneDate:'2026-03-10', status:'DONE', plan:'우선순위(High/Mid/Low) 기재', checker:'QA', checkDate:'2026-03-12' },
    { no:'CA_R02', project:'NHGBS', biz:'여신관리', gubun:'개발', stage:'설계', round:1, date:'2026-05-20', reviewer:'AI품질검토봇',
      artifact:'UI목록', file:'NHGBS-DV-DS01-UI목록_V1.0a_20260515.xlsx',
      loc:'시트:목록, Row 30', sev:'권고', dtype:'기타', persp:'산출물',
      desc:'화면 설명(비고) 컬럼 일부 공란', owner:'김명호', due:'2026-05-27',
      doneDate:'2026-05-24', status:'DONE', plan:'비고 보완', checker:'QA', checkDate:'2026-05-27' }
  ];

  // 시정조치관리대장(설계단계) — 실 PoC 샘플 기준 (NHEFS-PM-342-03 시정조치서 시트, 22행 · 개선 20/권고 2)
  var LEDGER = [
    { no:"CA_W01", project:"NHOB", biz:"개발산출물", gubun:"공통", stage:"설계", round:1, date:"2026-06-24", reviewer:"정혜윤", artifact:"요구사항정의서", file:"NHEFS-IC-AN06-요구사항정의서_V1.01.xlsx NHEFS-EA-AN06-요구사항정의서_V1.01.xlsx", loc:"기능", sev:"개선", dtype:"필수항목누락", desc:"1. 요구사항 출처 누락     - 해당 요구사항이 도출된 출처는 요구사항의 추적성을 확보하기 위한        필수 입력사항이므로 보완 필요함", owner:"", due:"", doneDate:"", status:"TARGET", plan:"", checker:"", checkDate:"" },
    { no:"CA_W02", project:"NHOB", biz:"프로젝트관리", gubun:"공통", stage:"설계", round:1, date:"2026-06-24", reviewer:"정혜윤", artifact:"WBS", file:"PMO-320-01-WBS((은행)2025 기업뱅킹 고도화 구축)_V1.0_20250519.xlsx", loc:"WBS", sev:"개선", dtype:"내용오류/불명확", desc:"1. 일부 항목에 대한 작성이 미완료됨     - 계획 항목 작성이 완료되지 않아 보완이 필요함", owner:"", due:"", doneDate:"", status:"TARGET", plan:"", checker:"", checkDate:"" },
    { no:"CA_W03", project:"NHOB", biz:"프로젝트관리", gubun:"공통", stage:"설계", round:1, date:"2026-06-24", reviewer:"정혜윤", artifact:"RoadMap 월별투입인력계획", file:"PMO-320-02-RoadMap((은행)2025 기업뱅킹 고도화 구축)_V1.0_20250519.pptx PMO-351-01_월별투입인력계획((은행)2025 기업뱅킹 고도화 구축)_V1.0_20250519.hwp", loc:"-", sev:"개선", dtype:"표준미준수", desc:"1. 공식 문서 표지 및 개정이력 누락    - 공식 산출물이므로 문서 표준에 맞게 관리 필요", owner:"", due:"", doneDate:"", status:"TARGET", plan:"", checker:"", checkDate:"" },
    { no:"CA_W04", project:"NHOB", biz:"개발산출물", gubun:"IC", stage:"설계", round:1, date:"2026-06-24", reviewer:"정혜윤", artifact:"프로세스정의서", file:"NHEFS-IC-AN04-프로세스정의서_V1.01.pptx", loc:"-", sev:"개선", dtype:"내용오류/불명확", desc:"1. 문서표지의 문서번호, 개정이력의 내용이 상이함    - 공식 산출물이므로 문서 표준에 맞게 관리 필요", owner:"", due:"", doneDate:"", status:"TARGET", plan:"", checker:"", checkDate:"" },
    { no:"CA_W05", project:"NHOB", biz:"개발산출물", gubun:"IC", stage:"설계", round:1, date:"2026-06-24", reviewer:"정혜윤", artifact:"프로세스정의서 요구사항추적표", file:"NHEFS-IC-AN04-프로세스정의서_V1.01.pptx NHEFS-PMO-310-02-요구사항추적표(통합)_(은행)2025 기업뱅킹 고도화 구축_V1.0_20250620.xlsx", loc:"전체", sev:"개선", dtype:"내용오류/불명확", desc:"1. 요구사항추적표와 프로세스정의서의 내용이 상이함     - 해당 요구사항의 액티비티ID 항목이 일치하지 않으므로 확인 후        보완이 필요함    - p5, 27, 29, 32, 34, 44, 49...  2. 판단이 필요한 항목이 처리만으로 표현되어 판단구분 표기가 필요함    - p7, 10, 33…", owner:"", due:"", doneDate:"", status:"TARGET", plan:"", checker:"", checkDate:"" },
    { no:"CA_W06", project:"NHOB", biz:"프로젝트관리", gubun:"공통", stage:"설계", round:1, date:"2026-06-25", reviewer:"양명규", artifact:"프로젝트산출물관리대장 테일러링 결과서", file:"PMO-121-02-테일러링결과서((은행)(2025 기업뱅킹 고도화 구축)_V1.0_20250519 PMO-141-02-프로젝트산출물관리대장((은행)(2025 기업뱅킹 고도화 구축)_V1.0_20250519", loc:"기업인터넷 기업스마트 CMS", sev:"개선", dtype:"내용오류/불명확", desc:"배치JOB목록이 테일러링 결과서와 산출물 관리대장 작업 결과물 차이 - 테일러링 결과서 : 배치정의 활동 수행하는 것으로 정의 - 산출물 관리대장 : 배치JOB목록 제외 되었음", owner:"", due:"", doneDate:"", status:"TARGET", plan:"", checker:"", checkDate:"" },
    { no:"CA_W07", project:"NHOB", biz:"프로젝트관리", gubun:"공통", stage:"설계", round:1, date:"2026-06-25", reviewer:"양명규", artifact:"프로젝트산출물관리대장 테일러링 결과서", file:"PMO-121-02-테일러링결과서((은행)(2025 기업뱅킹 고도화 구축)_V1.0_20250519 PMO-141-02-프로젝트산출물관리대장((은행)(2025 기업뱅킹 고도화 구축)_V1.0_20250519", loc:"기업인터넷 기업스마트 CMS AP_B2B", sev:"권고", dtype:"기타", desc:"이행계획서는 프로젝트 관리 관점에서 통합 구성을 권고.  (테일러링 사유에 통합적용. 프로젝트 관리 산출물로 조정하거나, 서브시스템 중 1개 업우에서 통합 구성) 서브시스템별 구분으로 프로젝트 차원에서 협의되었다면 현상태 유지하여 진행", owner:"", due:"", doneDate:"", status:"TARGET", plan:"", checker:"", checkDate:"" },
    { no:"CA_W08", project:"NHOB", biz:"개발산출물", gubun:"CM", stage:"설계", round:1, date:"2026-06-25", reviewer:"양명규", artifact:"물리ERD 테이블정의서", file:"NHEFS-CM-DS05-물리ERD_V1.01 NHEFS-CM-DS06-테이블정의서_V1.01", loc:"전체", sev:"개선", dtype:"필수항목누락", desc:"작성내용 없음", owner:"", due:"", doneDate:"", status:"TARGET", plan:"", checker:"", checkDate:"" },
    { no:"CA_W09", project:"NHOB", biz:"개발산출물", gubun:"CM", stage:"설계", round:1, date:"2026-06-25", reviewer:"양명규", artifact:"프로그램목록", file:"NHEFS-CM-DS07-프로그램목록_V1.01", loc:"전체", sev:"권고", dtype:"내용오류/불명확", desc:"서비스 구분은 UI, 프로그램 유형은 서비스, 컨트롤러 등 다양한 유형으로  표현됨. 프로그램에 대한 정보 일원화 필요 - PT컨트롤러, BT컨트롤러, BT서비스 , 서비스 , BT모델 등 유형항목으로 정의된 내용이 서비스 구분의 UI로 표현이 적절한지 재확인 필요 - 농협표준과 해당 프로젝트의 유형 정의에 차이가 있으므로 용어에 대한 정의가 표준화 영역에 부합되도록 기준 명확화 필요", owner:"", due:"", doneDate:"", status:"TARGET", plan:"", checker:"", checkDate:"" },
    { no:"CA_W10", project:"NHOB", biz:"개발산출물", gubun:"IC", stage:"설계", round:1, date:"2026-06-25", reviewer:"양명규", artifact:"프로그램목록", file:"NHEFS-IC-DS07-프로그램목록_V1.01", loc:"전체", sev:"개선", dtype:"표준미준수", desc:"서비스 구분과 프로그램 정보 유형 항목에 동일한 내용이나 정의된 용어의 차이가 있어 일관성 있게 조정 필요 :  UI / UI화면", owner:"", due:"", doneDate:"", status:"TARGET", plan:"", checker:"", checkDate:"" },
    { no:"CA_W11", project:"NHOB", biz:"개발산출물", gubun:"IC", stage:"설계", round:1, date:"2026-06-25", reviewer:"양명규", artifact:"물리ERD", file:"NHEFS-IC-DS05-물리ERD_V1.01", loc:"전체", sev:"개선", dtype:"표준미준수", desc:"물리ERD이나 논리ERD 내용으로 정의되어있음. 물리ERD는 테이블과 칼럼명이 정의되어야 함", owner:"", due:"", doneDate:"", status:"TARGET", plan:"", checker:"", checkDate:"" },
    { no:"CA_W12", project:"NHOB", biz:"개발산출물", gubun:"EA", stage:"설계", round:1, date:"2026-06-25", reviewer:"양명규", artifact:"물리ERD", file:"NHEFS-EA-DS06-테이블정의서_V1.01 NHEFS-EA-DS05-물리ERD_V1.01", loc:"전체", sev:"개선", dtype:"기타", desc:"물리ERD의 구분 (단위업무) 전자금융(B2B) 와 테이블정의서 단위업무 구분(NH다같이성장론) 의 일관성 유지", owner:"", due:"", doneDate:"", status:"TARGET", plan:"", checker:"", checkDate:"" },
    { no:"CA_W13", project:"NHOB", biz:"개발산출물", gubun:"EA", stage:"설계", round:1, date:"2026-06-25", reviewer:"양명규", artifact:"물리ERD", file:"NHEFS-EA-DS06-테이블정의서_V1.01 NHEFS-EA-DS05-물리ERD_V1.01", loc:"Y", sev:"개선", dtype:"표준미준수", desc:"필수여부 : Y  (작성가이드) NOTNULL", owner:"", due:"", doneDate:"", status:"TARGET", plan:"", checker:"", checkDate:"" },
    { no:"CA_W14", project:"NHOB", biz:"개발산출물", gubun:"SC", stage:"설계", round:1, date:"2026-06-25", reviewer:"양명규", artifact:"물리ERD", file:"NHEFS-SC-DS05-물리ERD_V1.01", loc:"전체", sev:"개선", dtype:"표준미준수", desc:"물리ERD이나 논리ERD 내용으로 정의되어있음. 물리ERD는 테이블과 칼럼명이 정의되어야 함", owner:"", due:"", doneDate:"", status:"TARGET", plan:"", checker:"", checkDate:"" },
    { no:"CA_W15", project:"NHOB", biz:"개발산출물", gubun:"SC", stage:"설계", round:1, date:"2026-06-25", reviewer:"양명규", artifact:"테이블정의서", file:"NHEFS-SC-DS01-테이블정의서_V1.0", loc:"단위업무", sev:"개선", dtype:"명명규칙위반", desc:"테이블목록의 단위업무 확인 : 기업인터넷", owner:"", due:"", doneDate:"", status:"TARGET", plan:"", checker:"", checkDate:"" },
    { no:"CA_W16", project:"NHOB", biz:"개발산출물", gubun:"SC", stage:"설계", round:1, date:"2026-06-25", reviewer:"양명규", artifact:"엔티티정의서", file:"NHEFS-SC-AN10-엔티티정의서_V1.01", loc:"엔티티명", sev:"개선", dtype:"표준미준수", desc:"엔티티명이 테이블명으로 작성되어 있음. 논리ERD와 맞게 수정 필요", owner:"", due:"", doneDate:"", status:"TARGET", plan:"", checker:"", checkDate:"" },
    { no:"CA_W17", project:"NHOB", biz:"개발산출물", gubun:"IC", stage:"설계", round:1, date:"2026-06-25", reviewer:"양명규", artifact:"엔티티정의서", file:"NHEFS-IC-AN10-엔티티정의서_V1.01", loc:"엔티티명", sev:"개선", dtype:"표준미준수", desc:"엔티티명이 테이블명으로 작성되어 있음. 논리ERD와 맞게 수정 필요", owner:"", due:"", doneDate:"", status:"TARGET", plan:"", checker:"", checkDate:"" },
    { no:"CA_W18", project:"NHOB", biz:"개발산출물", gubun:"EA", stage:"설계", round:1, date:"2026-06-25", reviewer:"양명규", artifact:"엔티티정의서 논리ERD", file:"NHEFS-EA-AN09-논리ERD_V1.01 NHEFS-EA-AN10-엔티티정의서_V1.01", loc:"속성명", sev:"개선", dtype:"기타", desc:"속성명 불일치. 확인 필요 - (엔티티정의서 기준) 변경일자, 변경일련번호 DB논리모델링에 대해서 ERD와 엔티티정의서 명칭이 내용에 대한 차이가 발생되지 않는 범위에서 반드시 일치해야 하는 필수 항목은 아님.", owner:"", due:"", doneDate:"", status:"TARGET", plan:"", checker:"", checkDate:"" },
    { no:"CA_W19", project:"NHOB", biz:"개발산출물", gubun:"EA", stage:"설계", round:1, date:"2026-06-25", reviewer:"양명규", artifact:"엔티티정의서 논리ERD", file:"NHEFS-EA-AN09-논리ERD_V1.01 NHEFS-EA-AN10-엔티티정의서_V1.01", loc:"속성명", sev:"개선", dtype:"기타", desc:"속성명 불일치. 확인 필요 - (엔티티정의서 기준 엔티티명) 외상매출채권_상생채권업체약정기본, 외상매출채권_상생채권업체약정이력 > 엔티티정의서의 속성명과 논리ERD의 속성 값 차이로 산출물간 일관성을 위해 확인 필요", owner:"", due:"", doneDate:"", status:"TARGET", plan:"", checker:"", checkDate:"" },
    { no:"CA_W20", project:"NHOB", biz:"개발산출물", gubun:"CM", stage:"설계", round:1, date:"2026-06-25", reviewer:"양명규", artifact:"엔티티정의서 논리ERD", file:"NHEFS-CM-AN09-논리ERD_V1.01 NHEFS-CM-AN10-엔티티정의서_V1.01", loc:"속성명", sev:"개선", dtype:"필수항목누락", desc:"작성내용 없음", owner:"", due:"", doneDate:"", status:"TARGET", plan:"", checker:"", checkDate:"" },
    { no:"CA_W21", project:"NHOB", biz:"프로젝트관리", gubun:"위험관리", stage:"설계", round:1, date:"2026-06-25", reviewer:"양명규", artifact:"위험-이슈-실행항목", file:"4.(은행)2025 기업뱅킹 고도화 구축(05주차)_위험-이슈-실행항목_V1.0_20250619", loc:"위험항목, 위험평가, 위험완롸/대응계획", sev:"개선", dtype:"기타", desc:"프로젝트 계획 수립시 위험을 식별하고 완화/대응 계획을 수립하기로 계획되었으나 위험관리 진행 내역 없음", owner:"", due:"", doneDate:"", status:"TARGET", plan:"", checker:"", checkDate:"" },
    { no:"CA_W22", project:"NHOB", biz:"프로젝트관리", gubun:"요구사항추적관리", stage:"설계", round:1, date:"2026-06-25", reviewer:"양명규", artifact:"요구사항추적표(비기능)", file:"NHEFS-PMO-310-02-요구사항추적표(통합)_(은행)2025 기업뱅킹 고도화 구축_V1.0_20250620", loc:"요구사항(비기능)", sev:"개선", dtype:"표준미준수", desc:"요구사항 추적표(기능)에 비기능 요구사항을 기록.. 비기능 요구사항은 요구사항추적표(비기능)에 표현", owner:"", due:"", doneDate:"", status:"TARGET", plan:"", checker:"", checkDate:"" }
  ];

  // 시스템 공지
  var NOTICES = [
    { date:'2026-07-07', title:'[공지] QA-Claude S2 Claude 검증 엔진 배포 — 유형 자동인식 지원' },
    { date:'2026-07-06', title:'[안내] 설계단계 산출물 4종 체크리스트 회귀 테스트 완료' },
    { date:'2026-07-03', title:'[점검] 07/09 02:00~04:00 파일 파서 정기 점검' },
    { date:'2026-06-30', title:'[공지] 명명규칙 위반 자동검출 규칙 4종 추가' },
    { date:'2026-06-27', title:'[안내] PoC 시정조치 36건 회귀 케이스 등록' }
  ];

  // Action Item
  var ACTIONS = [
    { id:'AI-118', title:'배치설계서 개선(ERROR) 3건 재검증 요청', owner:'김명호', due:'2026-07-09', state:'진행' },
    { id:'AI-117', title:'인터페이스설계서 명명규칙 정정', owner:'김명호', due:'2026-07-09', state:'진행' },
    { id:'AI-115', title:'UI목록 Level2명 연쇄 오타 정정(3종)', owner:'김명호', due:'2026-07-09', state:'대기' },
    { id:'AI-110', title:'요구사항정의서 REQ-032 중복 해소', owner:'김명호', due:'2026-07-03', state:'완료' }
  ];

  // 승인 대기(결재)
  var APPROVALS = [
    { id:5032, type:'QA 예외신청', target:'NHOB 배치설계서(DS09) 개선 3건', req:'김명호', date:'2026-07-07', state:'대기' },
    { id:5231, type:'단계말 검토결과서', target:'NHGBS 설계단계 최종 검토결과서 발급', req:'김명호', date:'2026-07-06', state:'대기' }
  ];

  // 현황/통계 전용 — 전체 프로젝트 현황 장표(10건). 실 3건 + 시연용 7건.
  // 단계 진행: done(완료) / run(진행중) / wait(예정)
  var STAT_PROJECTS = [
    { code:'NHOB',  name:'(은행) 국외지점 전산시스템 개선', pm:'김명호', mgmt:'done', analysis:'done', design:'run',  test:'wait', deploy:'wait', reviews:13, improve:6, recommend:5, doneRate:35 },
    { code:'NHUL',  name:'(은행) U2L 전환 구축',           pm:'김명호', mgmt:'done', analysis:'done', design:'run',  test:'wait', deploy:'wait', reviews:9,  improve:5, recommend:4, doneRate:44 },
    { code:'NHGBS', name:'(은행) GBS 여신시스템 구축',      pm:'김명호', mgmt:'done', analysis:'done', design:'done', test:'run',  deploy:'wait', reviews:10, improve:0, recommend:6, doneRate:100 },
    { code:'NHFX',  name:'(은행) 외화송금 통합관리 구축',   pm:'박서준', mgmt:'done', analysis:'run',  design:'wait', test:'wait', deploy:'wait', reviews:6,  improve:4, recommend:2, doneRate:33 },
    { code:'NHPB',  name:'(은행) 프라이빗뱅킹 상담 고도화', pm:'이도현', mgmt:'done', analysis:'done', design:'run',  test:'wait', deploy:'wait', reviews:11, improve:3, recommend:5, doneRate:58 },
    { code:'NHCD',  name:'(카드) 정산시스템 재구축',        pm:'최유진', mgmt:'run',  analysis:'wait', design:'wait', test:'wait', deploy:'wait', reviews:3,  improve:2, recommend:1, doneRate:20 },
    { code:'NHTR',  name:'(은행) 퇴직연금 관리 고도화',     pm:'박서준', mgmt:'done', analysis:'run',  design:'wait', test:'wait', deploy:'wait', reviews:7,  improve:3, recommend:3, doneRate:45 },
    { code:'NHIB',  name:'(은행) 기업뱅킹 포털 개선',       pm:'이도현', mgmt:'done', analysis:'done', design:'run',  test:'wait', deploy:'wait', reviews:12, improve:4, recommend:6, doneRate:62 },
    { code:'NHSC',  name:'(은행) 자산 스코어링 엔진 구축',  pm:'김명호', mgmt:'done', analysis:'done', design:'done', test:'done', deploy:'run',  reviews:15, improve:1, recommend:7, doneRate:92 },
    { code:'NHDW',  name:'(그룹) 데이터웨어하우스 전환',    pm:'최유진', mgmt:'done', analysis:'done', design:'done', test:'run',  deploy:'wait', reviews:14, improve:2, recommend:8, doneRate:78 }
  ];

  // 현재 로그인 사용자 (session.js) — 없으면 null
  function currentUser(){ return (window.QAGPT && window.QAGPT.getUser()) || null; }

  // ── 사용자 생성 프로젝트 저장소 (localStorage) ──────────
  //   신청 화면에서 등록한 프로젝트를 브라우저에 저장 → 목록·드롭다운에 반영
  var PROJ_KEY = 'qagpt.projects';
  function storedProjects(){
    try { return JSON.parse(localStorage.getItem(PROJ_KEY) || '[]'); }
    catch (e) { return []; }
  }
  function saveStoredProjects(arr){
    try { localStorage.setItem(PROJ_KEY, JSON.stringify(arr)); } catch (e) {}
  }

  // ── 프로젝트 상태 오버라이드 (관리자 승인/반려) ──────────
  //   기본 목업/저장 프로젝트의 status를 승인 처리로 변경 → localStorage 반영
  var STATUS_KEY = 'qagpt.projstatus';
  function statusOverrides(){
    try { return JSON.parse(localStorage.getItem(STATUS_KEY) || '{}'); }
    catch (e) { return {}; }
  }
  function setProjectStatus(id, status, approver){
    var today = new Date().toISOString().slice(0,10);
    var o = statusOverrides();
    o[id] = { status: status, approver: approver || '', at: today };
    try { localStorage.setItem(STATUS_KEY, JSON.stringify(o)); } catch (e) {}

    // 승인/반려 → 해당 프로젝트 PM에게 알림 생성
    var proj = storedProjects().concat(PROJECTS).filter(function(p){ return String(p.id) === String(id); })[0];
    if (proj && (status === 'ACTIVE' || status === 'APPROVED' || status === 'REJECTED')) {
      var approved = (status !== 'REJECTED');
      addNotification({
        to: proj.pm,
        type: approved ? 'approved' : 'rejected',
        title: approved ? '프로젝트 승인 완료' : '프로젝트 반려',
        message: '[' + proj.code + '] ' + proj.name + ' 이(가) ' +
                 (approved ? '승인되어 검증을 진행할 수 있습니다.' : '반려되었습니다. 내용을 확인하세요.'),
        projectId: proj.id, projectCode: proj.code,
        by: approver || '관리자', at: today
      });
    }
  }

  // ── 대기(진행중) 검증 저장소 (localStorage) ──────────────
  //   업로드 즉시 '검증중' 행을 만들어 검증 현황에서 Phase 애니메이션을 보여준다.
  //   완료·판정은 서버 실제 결과(/api/reviews)로만 확정하며, 매칭되면 여기서 제거한다.
  var PENDING_KEY = 'qagpt.pendingReviews';
  var RAMP_MS = 60000;              // 1→99% 램프 소요(약 1분)
  var PENDING_TTL_MS = 30 * 60000;  // 좀비 대기행 자동 정리(30분)
  function pendingRaw(){
    try { return JSON.parse(localStorage.getItem(PENDING_KEY) || '[]'); }
    catch (e) { return []; }
  }
  function savePending(arr){
    try { localStorage.setItem(PENDING_KEY, JSON.stringify(arr)); } catch (e) {}
  }
  // TTL 초과 항목 정리 후 반환
  function pendingReviews(){
    var now = Date.now();
    var raw = pendingRaw();
    var arr = raw.filter(function(r){ return now - (r.startedAt || 0) < PENDING_TTL_MS; });
    if (arr.length !== raw.length) savePending(arr);
    return arr;
  }
  function pendingReviewsFor(code){
    return pendingReviews().filter(function(r){ return r.projectCode === code; });
  }
  function addPendingReview(input){
    var arr = pendingRaw();
    // 음수 임시 id (실제 reviewId와 충돌 방지)
    var minId = arr.reduce(function(m, r){ return Math.min(m, r.tempId || 0); }, 0);
    var rec = {
      tempId: minId - 1,
      projectId: input.projectId != null ? input.projectId : null,
      projectCode: input.projectCode || '미지정',
      projectName: input.projectName || input.projectCode || '미지정 프로젝트',
      stage: input.stage || '-',
      fileName: input.fileName || '',
      artifactLabel: input.artifactLabel
        || (input.fileName ? String(input.fileName).replace(/\.[^.]+$/, '') : '업로드 산출물'),
      round: input.round || 1,
      startedAt: input.startedAt || Date.now(),
      baselineMaxReviewId: input.baselineMaxReviewId || 0
    };
    arr.push(rec);
    savePending(arr);
    return rec;
  }
  function removePendingReview(tempId){
    savePending(pendingRaw().filter(function(r){ return r.tempId !== tempId; }));
  }
  // 경과시간 → 진행%/phase. 상한 99%(실제 결과 도착 전까지 100% 금지 — 무한 대기).
  function phaseProgress(startedAt, now){
    var elapsed = Math.max(0, (now || Date.now()) - (startedAt || 0));
    var frac = Math.min(1, elapsed / RAMP_MS);        // 약 1분 동안 선형으로 천천히 1→99
    var pct = Math.max(1, Math.min(99, Math.round(1 + frac * 98)));
    var phase = Math.max(1, Math.min(4, Math.ceil(pct / 25)));
    return { pct: pct, phase: phase };
  }

  // ── 단계말 검토결과서 발급 (QA 승인) ────────────────────
  var REPORT_KEY = 'qagpt.reports';
  function issuedReports(){
    try { return JSON.parse(localStorage.getItem(REPORT_KEY) || '{}'); }
    catch (e) { return {}; }
  }
  function issuedReport(projectId, stage){
    return issuedReports()[projectId + '/' + stage] || null;
  }
  function issueReport(proj, stage, by){
    var today = new Date().toISOString().slice(0,10);
    var o = issuedReports();
    o[proj.id + '/' + stage] = { by: by || 'QA', at: today };
    try { localStorage.setItem(REPORT_KEY, JSON.stringify(o)); } catch (e) {}
    addNotification({
      to: proj.pm,
      type: 'report',
      title: '단계말 검토결과서 발급',
      message: '[' + proj.code + '] ' + stage + '단계 검토결과서가 QA 승인되어 발급되었습니다.',
      projectId: proj.id, projectCode: proj.code,
      by: by || 'QA', at: today
    });
    return o[proj.id + '/' + stage];
  }

  // ── 알림 (승인 결과 등) ─────────────────────────────────
  var NOTI_KEY = 'qagpt.notifications';
  function notifications(){
    try { return JSON.parse(localStorage.getItem(NOTI_KEY) || '[]'); }
    catch (e) { return []; }
  }
  function saveNotifications(arr){
    try { localStorage.setItem(NOTI_KEY, JSON.stringify(arr)); } catch (e) {}
  }
  function addNotification(n){
    var arr = notifications();
    n.id = 'N' + Date.now() + Math.floor(Math.random()*1000);
    n.read = false;
    arr.unshift(n);
    saveNotifications(arr);
    return n;
  }
  function notificationsFor(name){
    return notifications().filter(function(n){ return n.to === name; });
  }
  function unreadCountFor(name){
    return notificationsFor(name).filter(function(n){ return !n.read; }).length;
  }
  function markAllReadFor(name){
    var arr = notifications();
    arr.forEach(function(n){ if (n.to === name) n.read = true; });
    saveNotifications(arr);
  }
  function removeNotification(id){
    saveNotifications(notifications().filter(function(n){ return n.id !== id; }));
  }

  // 기본 목업 + 사용자 생성분 (사용자 생성분을 위로), 상태 오버라이드 적용
  function allProjects(){
    var o = statusOverrides();
    return storedProjects().concat(PROJECTS).map(function (p){
      var ov = o[p.id];
      if (!ov) return p;
      var np = {}; for (var k in p) np[k] = p[k];
      np.status = ov.status; np.approver = ov.approver; np.approvedAt = ov.at;
      // 승인 시 최소 진행 표시
      if (ov.status === 'ACTIVE' && (!np.stage || np.stage === '-')) { np.stage = '관리'; }
      return np;
    });
  }

  // 승인 대기(REQUESTED) 프로젝트
  function pendingProjects(){
    return allProjects().filter(function (p){ return p.status === 'REQUESTED'; });
  }

  // 산출물 검증 가능 프로젝트 = 승인(ACTIVE/APPROVED) + 역할 필터
  function validatableProjects(){
    return visibleProjects().filter(function (p){
      return p.status === 'ACTIVE' || p.status === 'APPROVED';
    });
  }

  // 프로젝트별 시정조치(결함) 목록
  function defectsFor(projectCode){
    return DEFECTS.filter(function (d){ return d.project === projectCode; });
  }

  // 새 프로젝트 등록 → 저장 후 생성된 객체 반환
  function addProject(input){
    var u = currentUser();
    var list = allProjects();
    var maxId = list.reduce(function (m, p){ return Math.max(m, p.id || 0); }, 1000);
    var proj = {
      id: maxId + 1,
      code: input.code,
      name: input.name,
      pm: (u && u.name) || '김명호',
      status: 'REQUESTED',
      stage: '-',
      progress: 0,
      start: input.start || '',
      end: input.end || ''
    };
    var arr = storedProjects();
    arr.unshift(proj);
    saveStoredProjects(arr);
    return proj;
  }

  // 역할별 조회 가능 프로젝트
  //   PM  → 본인이 담당(pm === 이름)한 프로젝트만
  //   그 외(QA·관리자·고객사·일반) → 전체
  function visibleProjects(){
    var u = currentUser();
    var base = allProjects();
    if (u && u.role === 'PM') {
      return base.filter(function (p){ return p.pm === u.name; });
    }
    return base;
  }

  // 조회 가능 프로젝트에 속한 검증 이력만 반환 (PM=본인 프로젝트, 그 외=전체)
  function visibleReviews(){
    var codes = visibleProjects().map(function (p){ return p.code; });
    return REVIEWS.filter(function (r){ return codes.indexOf(r.project) !== -1; });
  }

  // 백엔드 /api/projects 를 단일 소스로 사용(표시필드 pm/stage/progress/기간은 목업 코드매칭으로 보강).
  //   화면 간 프로젝트 목록 불일치를 없애기 위해 목록 화면들이 공통으로 이 함수를 쓴다.
  //   백엔드 미연결(정적 프리뷰) 시 목업 전체로 폴백. 결과를 cb(list)로 전달.
  function loadProjects(cb){
    fetch('/api/projects')
      .then(function (r){ if (!r.ok) throw new Error('http'); return r.json(); })
      .then(function (api){
        var byCode = {}; allProjects().forEach(function (m){ byCode[m.code] = m; });
        cb((api || []).map(function (p){
          var m = byCode[p.code] || {};
          return { id:p.id, code:p.code, name:p.name || m.name || p.code, status:p.status,
            pm:m.pm || '-', stage:m.stage || '-', progress:(m.progress != null ? m.progress : 0),
            start:m.start || '', end:m.end || '' };
        }));
      })
      .catch(function (){ cb(allProjects()); });
  }

  // 검증 대상(승인 완료) 프로젝트 — 검증 현황·단계별 검증 공통 단일 소스.
  //   백엔드 /api/projects 에서 ACTIVE/APPROVED 만, 같은 code 는 1건으로 합쳐(중복 EFS 등)
  //   두 검증 화면의 프로젝트 목록을 정확히 일치시킨다. 백엔드 미연결 시 목업 폴백.
  function loadValidatableProjects(cb){
    fetch('/api/projects')
      .then(function (r){ if (!r.ok) throw new Error('http'); return r.json(); })
      .then(function (api){
        var seen = {}, out = [];
        (api || []).forEach(function (p){
          if (p.status !== 'ACTIVE' && p.status !== 'APPROVED') return;
          if (seen[p.code]) return;
          seen[p.code] = 1;
          out.push({ id:p.id, code:p.code, name:p.name || p.code, status:p.status });
        });
        cb(out.length ? out : validatableProjects());
      })
      .catch(function (){ cb(validatableProjects()); });
  }

  return {
    ARTIFACTS: ARTIFACTS, PROJECTS: PROJECTS, REVIEWS: REVIEWS, DEFECTS: DEFECTS, LEDGER: LEDGER,
    NOTICES: NOTICES, ACTIONS: ACTIONS, APPROVALS: APPROVALS, STAT_PROJECTS: STAT_PROJECTS,
    currentUser: currentUser, visibleProjects: visibleProjects, visibleReviews: visibleReviews,
    allProjects: allProjects, addProject: addProject, loadProjects: loadProjects,
    loadValidatableProjects: loadValidatableProjects,
    pendingProjects: pendingProjects, setProjectStatus: setProjectStatus, validatableProjects: validatableProjects,
    defectsFor: defectsFor,
    pendingReviews: pendingReviews, pendingReviewsFor: pendingReviewsFor,
    addPendingReview: addPendingReview, removePendingReview: removePendingReview,
    phaseProgress: phaseProgress,
    issuedReport: issuedReport, issueReport: issueReport,
    addNotification: addNotification,
    notificationsFor: notificationsFor, unreadCountFor: unreadCountFor, markAllReadFor: markAllReadFor,
    removeNotification: removeNotification,
    esc: function (s){ return String(s == null ? '' : s).replace(/[&<>"']/g, function(c){
      return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]; }); },
    sevBadge: function (sev){ return '<span class="badge '+sev+'">'+sev+'</span>'; },
    stateHtml: function (status){
      var m = { COMPLETED:['done','완료'], RUNNING:['run','검증중'], PENDING:['wait','대기'],
                FAILED:['fail','실패'], TARGET:['wait','대상'], IN_PROGRESS:['run','조치중'],
                DONE:['done','완료'], ACTIVE:['done','진행'], APPROVED:['run','승인'],
                REQUESTED:['wait','승인대기'], REJECTED:['fail','반려'] };
      var v = m[status] || ['wait', status];
      return '<span class="state '+v[0]+'">'+v[1]+'</span>';
    }
  };
})();
