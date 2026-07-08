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

  // 프로젝트 목록
  var PROJECTS = [
    { id:1024, code:'NBIA', name:'(은행) 투자자문 관리시스템 구축', pm:'김명호', status:'ACTIVE',
      stage:'설계', progress:72, start:'2025-11-03', end:'2026-07-31' },
    { id:1019, code:'HFMS', name:'하나 펀드 판매관리 고도화', pm:'박서준', status:'ACTIVE',
      stage:'분석', progress:41, start:'2026-01-06', end:'2026-09-30' },
    { id:1007, code:'KRWM', name:'우리 자산관리 포털 재구축', pm:'이도현', status:'APPROVED',
      stage:'관리', progress:12, start:'2026-03-02', end:'2026-12-20' },
    { id:1002, code:'SBPO', name:'신한 백오피스 통합', pm:'최유진', status:'REQUESTED',
      stage:'-', progress:0, start:'2026-05-11', end:'2027-01-31' },
    { id:1031, code:'NBPB', name:'(은행) 프라이빗뱅킹 상담시스템 구축', pm:'김명호', status:'ACTIVE',
      stage:'분석', progress:35, start:'2026-02-10', end:'2026-10-30' },
    { id:1028, code:'NBLN', name:'(은행) 여신심사 자동화 플랫폼', pm:'김명호', status:'APPROVED',
      stage:'관리', progress:8, start:'2026-04-01', end:'2027-02-28' }
  ];

  // 검증(회차) 이력
  var REVIEWS = [
    { id:341, project:'NBIA', artifact:'배치설계서', code:'DS09', stage:'설계', round:2, status:'COMPLETED',
      passed:false, error:3, warn:5, date:'2026-07-06' },
    { id:340, project:'NBIA', artifact:'인터페이스설계서', code:'DS10', stage:'설계', round:1, status:'COMPLETED',
      passed:false, error:2, warn:4, date:'2026-07-06' },
    { id:338, project:'NBIA', artifact:'UI목록', code:'DS01', stage:'설계', round:1, status:'RUNNING',
      passed:null, error:0, warn:0, date:'2026-07-07' },
    { id:335, project:'NBIA', artifact:'프로그램목록', code:'DS07', stage:'설계', round:3, status:'COMPLETED',
      passed:true, error:0, warn:1, date:'2026-07-05' },
    { id:322, project:'NBIA', artifact:'배치Job목록', code:'AN07', stage:'분석', round:2, status:'COMPLETED',
      passed:true, error:0, warn:2, date:'2026-06-28' },
    { id:318, project:'NBIA', artifact:'요구사항정의서', code:'AN06', stage:'분석', round:1, status:'COMPLETED',
      passed:false, error:6, warn:3, date:'2026-06-27' },

    // ── NBPB (프라이빗뱅킹 상담시스템 · PM 김명호) ──
    { id:372, project:'NBPB', artifact:'프로세스정의서', code:'AN04', stage:'분석', round:1, status:'COMPLETED',
      passed:false, error:3, warn:4, date:'2026-07-02' },
    { id:371, project:'NBPB', artifact:'요구사항추적표', code:'PM-310-01', stage:'관리', round:2, status:'COMPLETED',
      passed:true, error:0, warn:1, date:'2026-06-19' },
    { id:370, project:'NBPB', artifact:'문서작성지침서', code:'PM-141-01', stage:'관리', round:1, status:'COMPLETED',
      passed:true, error:0, warn:0, date:'2026-06-18' },

    // ── NBLN (여신심사 자동화 · PM 김명호) ──
    { id:389, project:'NBLN', artifact:'요구사항추적표', code:'PM-310-01', stage:'관리', round:1, status:'RUNNING',
      passed:null, error:0, warn:0, date:'2026-07-07' },
    { id:388, project:'NBLN', artifact:'테일러링결과서', code:'PM-120-01', stage:'관리', round:1, status:'COMPLETED',
      passed:false, error:1, warn:2, date:'2026-07-04' },

    // ── HFMS (하나 펀드 판매관리 · PM 박서준) ──
    { id:410, project:'HFMS', artifact:'요구사항정의서', code:'AN06', stage:'분석', round:1, status:'COMPLETED',
      passed:false, error:4, warn:2, date:'2026-07-01' },
    { id:405, project:'HFMS', artifact:'문서작성지침서', code:'PM-141-01', stage:'관리', round:2, status:'COMPLETED',
      passed:true, error:0, warn:1, date:'2026-06-20' },

    // ── KRWM (우리 자산관리 포털 · PM 이도현) ──
    { id:362, project:'KRWM', artifact:'요구사항추적표', code:'PM-310-01', stage:'관리', round:1, status:'RUNNING',
      passed:null, error:0, warn:0, date:'2026-07-07' },
    { id:360, project:'KRWM', artifact:'테일러링결과서', code:'PM-120-01', stage:'관리', round:1, status:'COMPLETED',
      passed:false, error:2, warn:3, date:'2026-06-25' }
  ];

  // 시정조치관리대장 (PoC 36건 성격 반영 — 대표 표본)
  var DEFECTS = [
    { no:'CA_P01', biz:'프로젝트관리', gubun:'관리', date:'2026-06-27', reviewer:'AI품질검토봇',
      artifact:'문서작성지침서', file:'NBIA-PM-141-01_문서작성지침서_v1.0.hwpx',
      loc:'표지', sev:'개선', dtype:'표준미준수', persp:'산출물',
      desc:'제·개정일자가 프로젝트 관리단계 기간(2025-11-03~) 이전으로 기재됨',
      owner:'김명호', due:'2026-07-02', doneDate:'', status:'IN_PROGRESS', plan:'표지 개정일자 정정',
      checker:'', checkDate:'' },
    { no:'CA_P04', biz:'프로젝트관리', gubun:'관리', date:'2026-06-27', reviewer:'AI품질검토봇',
      artifact:'테일러링결과서', file:'NBIA-PM-120-01_테일러링결과서_v1.0.xlsx',
      loc:'시트:설계, Row 12', sev:'개선', dtype:'필수항목누락', persp:'산출물',
      desc:'설계단계 산출물 "UI레이아웃" 담당부서 미기재', owner:'김명호', due:'2026-07-03',
      doneDate:'2026-07-01', status:'DONE', plan:'담당부서(UX팀) 보완', checker:'QA', checkDate:'2026-07-02' },
    { no:'CA_W03', biz:'투자자문', gubun:'개발', date:'2026-06-27', reviewer:'AI품질검토봇',
      artifact:'요구사항정의서', file:'NBIA-DV-AN06-요구사항정의서(투자자문)_V1.0a_20260115.xlsx',
      loc:'시트:요구사항, Row 45 / ID REQ-032', sev:'개선', dtype:'중복', persp:'산출물',
      desc:'요구사항 ID REQ-032 가 2개 행에 중복 정의됨', owner:'김명호', due:'2026-07-03',
      doneDate:'', status:'TARGET', plan:'중복 행 통합/재채번', checker:'', checkDate:'' },
    { no:'CA_W07', biz:'투자자문', gubun:'개발', date:'2026-06-27', reviewer:'AI품질검토봇',
      artifact:'요구사항정의서', file:'NBIA-DV-AN06-요구사항정의서(투자자문)_V1.0a_20260115.xlsx',
      loc:'시트:요구사항, Row 60', sev:'권고', dtype:'기타', persp:'산출물',
      desc:'요구사항 근거(출처) 컬럼이 공란 — 추적성 확인 곤란', owner:'김명호', due:'2026-07-05',
      doneDate:'', status:'TARGET', plan:'출처(회의록/RFP) 보완 권고', checker:'', checkDate:'' },
    { no:'CA_W14', biz:'투자자문', gubun:'개발', date:'2026-06-28', reviewer:'AI품질검토봇',
      artifact:'배치Job목록', file:'NBIA-DV-AN07-배치Job목록(투자자문)_V1.0a_20260115.xlsx',
      loc:'시트:내용, Row 3~62', sev:'개선', dtype:'필수항목누락', persp:'산출물',
      desc:'배치 Job 다수에 "실패 시 처리방안" 미정의', owner:'김명호', due:'2026-07-04',
      doneDate:'2026-07-03', status:'DONE', plan:'실패처리(재시도/알림) 컬럼 작성', checker:'QA', checkDate:'2026-07-04' },
    { no:'CA_W21', biz:'투자자문', gubun:'개발', date:'2026-07-06', reviewer:'AI품질검토봇',
      artifact:'UI목록', file:'NBIA-DV-DS01-UI목록(투자자문)_v1.0_20260212.xlsx',
      loc:'시트:목록, Row 8 / Level2명', sev:'개선', dtype:'내용오류·불명확', persp:'프로세스',
      desc:'Level2명 오타 "투좌자문" → 메뉴구조도·프로그램목록으로 연쇄 전염', owner:'김명호',
      due:'2026-07-09', doneDate:'', status:'TARGET', plan:'3개 산출물 동시 정정', checker:'', checkDate:'' },
    { no:'CA_W28', biz:'투자자문', gubun:'개발', date:'2026-07-06', reviewer:'AI품질검토봇',
      artifact:'인터페이스설계서', file:'NBIA-DV-DS10-인터페이스설계서_운용사유형별지표분석(FDTPB203)_v1.0.xlsx',
      loc:'문서번호/파일명', sev:'개선', dtype:'표준미준수', persp:'산출물',
      desc:'파일명 날짜(YYYYMMDD) 누락 · 버전표기 소문자 v1.0 (표준 V1.0a)', owner:'김명호',
      due:'2026-07-09', doneDate:'', status:'IN_PROGRESS', plan:'명명규칙 준수 재저장', checker:'', checkDate:'' },
    { no:'CA_W31', biz:'투자자문', gubun:'개발', date:'2026-07-06', reviewer:'AI품질검토봇',
      artifact:'배치설계서', file:'NBIA-DV-DS09-배치설계서(투자자문)_V1.0a_20260115.xlsx',
      loc:'시트:설계, Row 22', sev:'권고', dtype:'기타', persp:'산출물',
      desc:'배치 소요시간(예상)이 일부 Job에만 기재 — 상세화 부족', owner:'김명호',
      due:'2026-07-10', doneDate:'', status:'TARGET', plan:'전 Job 소요시간 산정 권고', checker:'', checkDate:'' },

    // ── NBPB (프라이빗뱅킹 상담시스템 · PM 김명호) ──
    { no:'CA_W41', biz:'프라이빗뱅킹', gubun:'개발', date:'2026-07-02', reviewer:'AI품질검토봇', project:'NBPB',
      artifact:'프로세스정의서', file:'NBPB-DV-AN04-프로세스정의서(상담)_V1.0_20260220.xlsx',
      loc:'시트:흐름, Row 5', sev:'개선', dtype:'필수항목누락', persp:'산출물',
      desc:'프로세스 시작/종료 지점 및 Input/Output 미표기', owner:'김명호', due:'2026-07-08',
      doneDate:'', status:'IN_PROGRESS', plan:'흐름 표준(시작/종료·I/O) 보완', checker:'', checkDate:'' },
    { no:'CA_W43', biz:'프라이빗뱅킹', gubun:'개발', date:'2026-07-02', reviewer:'AI품질검토봇', project:'NBPB',
      artifact:'프로세스정의서', file:'NBPB-DV-AN04-프로세스정의서(상담)_V1.0_20260220.xlsx',
      loc:'시트:흐름, Row 12', sev:'권고', dtype:'기타', persp:'프로세스',
      desc:'액티비티 ID 표기 규칙이 문서 내에서 불일치', owner:'김명호', due:'2026-07-10',
      doneDate:'', status:'TARGET', plan:'액티비티 ID 체계 통일 권고', checker:'', checkDate:'' },

    // ── NBLN (여신심사 자동화 · PM 김명호) ──
    { no:'CA_P07', biz:'여신심사', gubun:'관리', date:'2026-07-04', reviewer:'AI품질검토봇', project:'NBLN',
      artifact:'테일러링결과서', file:'NBLN-PM-120-01_테일러링결과서_v1.0.xlsx',
      loc:'시트:분석, Row 8', sev:'개선', dtype:'필수항목누락', persp:'산출물',
      desc:'분석단계 산출물 "인터페이스정의서" 담당부서 미기재', owner:'김명호', due:'2026-07-09',
      doneDate:'', status:'TARGET', plan:'담당부서 지정 보완', checker:'', checkDate:'' },

    // ── HFMS (하나 펀드 판매관리 · PM 박서준) ──
    { no:'CA_W51', biz:'펀드판매', gubun:'개발', date:'2026-07-01', reviewer:'AI품질검토봇', project:'HFMS',
      artifact:'요구사항정의서', file:'HFMS-DV-AN06-요구사항정의서(펀드)_V1.0a_20260118.xlsx',
      loc:'시트:요구사항, Row 22 / ID', sev:'개선', dtype:'표준미준수', persp:'산출물',
      desc:'요구사항 ID 체계가 표준(REQ-###)과 불일치', owner:'박서준', due:'2026-07-07',
      doneDate:'2026-07-06', status:'DONE', plan:'ID 재채번', checker:'QA', checkDate:'2026-07-07' },
    { no:'CA_W52', biz:'펀드판매', gubun:'개발', date:'2026-07-01', reviewer:'AI품질검토봇', project:'HFMS',
      artifact:'요구사항정의서', file:'HFMS-DV-AN06-요구사항정의서(펀드)_V1.0a_20260118.xlsx',
      loc:'시트:요구사항, Row 30', sev:'권고', dtype:'기타', persp:'산출물',
      desc:'요구사항 우선순위 컬럼 다수 공란', owner:'박서준', due:'2026-07-09',
      doneDate:'', status:'TARGET', plan:'우선순위(High/Mid/Low) 기재 권고', checker:'', checkDate:'' }
  ];
  // 프로젝트 미지정(기존 NBIA PoC 표본) 기본값 부여
  DEFECTS.forEach(function(d){ if(!d.project) d.project = 'NBIA'; });

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
    { id:1002, type:'프로젝트 신청', target:'신한 백오피스 통합(SBPO)', req:'최유진', date:'2026-07-05', state:'대기' },
    { id:341, type:'QA 예외신청', target:'배치설계서(DS09) 개선 3건', req:'김명호', date:'2026-07-06', state:'대기' },
    { id:335, type:'단계말 검토결과서', target:'분석단계 최종 검토결과서 발급', req:'김명호', date:'2026-07-05', state:'대기' }
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

  return {
    ARTIFACTS: ARTIFACTS, PROJECTS: PROJECTS, REVIEWS: REVIEWS, DEFECTS: DEFECTS,
    NOTICES: NOTICES, ACTIONS: ACTIONS, APPROVALS: APPROVALS,
    currentUser: currentUser, visibleProjects: visibleProjects, visibleReviews: visibleReviews,
    allProjects: allProjects, addProject: addProject,
    pendingProjects: pendingProjects, setProjectStatus: setProjectStatus, validatableProjects: validatableProjects,
    defectsFor: defectsFor,
    issuedReport: issuedReport, issueReport: issueReport,
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
