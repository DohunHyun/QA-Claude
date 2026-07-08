/* ============================================================
   QA-Claude 공통 레이아웃 — 상단 네비 + 좌측 사이드바 렌더링
   각 페이지 <body data-nav="..." data-page="...">로 활성 메뉴 지정
   ============================================================ */
(function () {
  // ── 로그인 사용자 (session.js) — 없으면 폴백 기본값 ──────
  var user = (window.QAGPT && window.QAGPT.getUser())
    || { name: '김명호', role: 'PM', org: 'NH디지털연구소', project: '투자자문 관리시스템(NBIA)' };
  var ROLE = user.role || 'PM';
  var READONLY_ROLES = ['고객사', '일반'];       // 조회 전용 역할

  // roles 미지정(undefined) = 전체 허용. 배열이면 포함 여부로 판정.
  function can(roles){ return !roles || roles.indexOf(ROLE) !== -1; }

  // 메뉴 정의 (상단 대분류 → 좌측 그룹/항목). roles = 접근 허용 역할
  var NAV = [
    {
      key: 'portal', label: '나의업무', href: 'dashboard.html',   // 전체
      side: [
        { g: 'MY PORTAL', items: [
          { p: 'dashboard', label: '나의 현황', href: 'dashboard.html' },
          { p: 'my-validations', label: '내 프로젝트 검증', href: 'my-validations.html', roles: ['PM','QA','관리자'] },
          { p: 'todo', label: '검증 TODO', href: 'dashboard.html#todo', roles: ['PM','QA'] },
          { p: 'approval', label: '나의 결재 / 승인', href: 'dashboard.html#approval', roles: ['QA','관리자'] }
        ]}
      ]
    },
    {
      key: 'project', label: '프로젝트', href: 'projects.html',   // 전체
      side: [
        { g: '프로젝트', items: [
          { p: 'projects', label: '프로젝트 목록', href: 'projects.html' },
          { p: 'project-new', label: '프로젝트 신청', href: 'project-new.html', roles: ['PM'] },
          { p: 'project-detail', label: '프로젝트 현황', href: 'project-detail.html' }
        ]},
        { g: '승인 관리', roles: ['관리자'], items: [
          { p: 'project-approval', label: '프로젝트 승인대기', href: 'project-approval.html', roles: ['관리자'] }
        ]}
      ]
    },
    {
      key: 'validate', label: '산출물 검증', href: 'validation-upload.html', roles: ['PM','QA'],
      side: [
        { g: '검증', items: [
          { p: 'validation-upload', label: '산출물 업로드', href: 'validation-upload.html', roles: ['PM','QA'] },
          { p: 'validation-progress', label: '검토 진행도', href: 'validation-progress.html' },
          { p: 'validation-result', label: '검증 결과', href: 'validation-result.html' }
        ]},
        { g: '단계별 검증', items: [
          { p: 'stage-mgmt', label: '관리산출물 (3)', href: 'validation-upload.html?stage=MANAGEMENT' },
          { p: 'stage-an', label: '분석단계 (4)', href: 'validation-upload.html?stage=ANALYSIS' },
          { p: 'stage-ds', label: '설계단계 (4)', href: 'validation-upload.html?stage=DESIGN' }
        ]}
      ]
    },
    {
      key: 'action', label: '시정조치', href: 'corrective-actions.html', roles: ['PM','QA','관리자','고객사'],
      side: [
        { g: '시정조치', items: [
          { p: 'corrective-actions', label: '시정조치관리대장', href: 'corrective-actions.html' },
          { p: 'action-status', label: '조치 현황 (대상/완료/잔여)', href: 'corrective-actions.html#status' }
        ]}
      ]
    },
    {
      key: 'report', label: '결과물', href: 'review-report.html', roles: ['PM','QA','관리자','고객사'],
      side: [
        { g: '결과물', items: [
          { p: 'review-report', label: '단계말 검토결과서', href: 'review-report.html' },
          { p: 'improved', label: 'AI 개선 산출물', href: 'review-report.html#improved' }
        ]}
      ]
    },
    {
      key: 'standard', label: '업무표준', href: 'standards.html',   // 전체
      side: [
        { g: '표준 체크리스트', items: [
          { p: 'standards', label: '전체 체크리스트 (12)', href: 'standards.html' },
          { p: 'std-mgmt', label: '관리산출물', href: 'standards.html?stage=MANAGEMENT' },
          { p: 'std-an', label: '분석단계', href: 'standards.html?stage=ANALYSIS' },
          { p: 'std-ds', label: '설계단계', href: 'standards.html?stage=DESIGN' }
        ]},
        { g: '판정 기준', items: [
          { p: 'std-defect', label: '결함 판정 체계', href: 'standards.html#defect' }
        ]}
      ]
    },
    {
      key: 'stats', label: '현황/통계', href: 'statistics.html',   // 전체
      side: [
        { g: '통계', items: [
          { p: 'statistics', label: '검증 현황', href: 'statistics.html' },
          { p: 'stat-defect', label: '결함유형 분석', href: 'statistics.html#defect' },
          { p: 'stat-round', label: '회차별 개선 추이', href: 'statistics.html#round' }
        ]}
      ]
    }
  ];

  var navKey = document.body.getAttribute('data-nav') || 'portal';
  var pageKey = document.body.getAttribute('data-page') || '';
  var active = NAV.filter(function (n) { return n.key === navKey; })[0] || NAV[0];

  function esc(s){ return String(s == null ? '' : s).replace(/[&<>"']/g, function(c){
    return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]; }); }

  // ── 상단 헤더 (역할별 메뉴 필터) ───────────────────────
  var topNav = NAV.filter(function (n) { return can(n.roles); }).map(function (n) {
    return '<a href="' + n.href + '"' + (n.key === navKey ? ' class="active"' : '') + '>' + esc(n.label) + '</a>';
  }).join('');

  var header =
    '<div class="top">' +
      '<a class="brand" href="dashboard.html"><span class="dot"></span>QA-Claude<small>품질검증 포털</small></a>' +
      '<nav class="top-nav">' + topNav + '</nav>' +
      '<div class="top-right">' +
        '<a class="bell" href="dashboard.html#notis" id="bellBtn" title="알림">🔔<span class="bell-count" id="bellCount"></span></a>' +
        '<div class="who"><b>' + esc(user.name) + '</b> ' + esc(user.role || '') + '<br>' +
          '<span>' + esc(user.org || '') + '</span></div>' +
        '<span class="clock" id="topClock">--:--:--</span>' +
        '<a class="logout" href="login.html" id="logoutBtn">로그아웃</a>' +
      '</div>' +
    '</div>';

  // ── 좌측 사이드바 (역할별 그룹·항목 필터) ──────────────
  var side = active.side.filter(function (grp) { return can(grp.roles); }).map(function (grp) {
    var visible = grp.items.filter(function (it) { return can(it.roles); });
    if (!visible.length) return '';
    var items = visible.map(function (it) {
      var on = it.p === pageKey ? ' class="active"' : '';
      return '<a href="' + it.href + '"' + on + '>' + esc(it.label) + '</a>';
    }).join('');
    return '<div class="side-group"><div class="g-title">' + esc(grp.g) + '</div>' + items + '</div>';
  }).join('');

  var sidebar =
    '<aside class="side">' +
      '<div class="search"><input type="text" placeholder="메뉴·산출물 검색"><button>검색</button></div>' +
      side +
    '</aside>';

  // ── 조립: 기존 .main 을 shell로 감싼다 ─────────────────
  var main = document.querySelector('.main');
  var shell = document.createElement('div');
  shell.className = 'shell';
  shell.innerHTML = sidebar;
  document.body.insertAdjacentHTML('afterbegin', header);
  if (main) {
    main.parentNode.insertBefore(shell, main);
    shell.appendChild(main);
  }

  // ── 시계 ──────────────────────────────────────────────
  function tick(){
    var d = new Date();
    var p = function(n){ return (n<10?'0':'')+n; };
    var el = document.getElementById('topClock');
    if (el) el.textContent = p(d.getHours())+':'+p(d.getMinutes())+':'+p(d.getSeconds());
  }
  tick(); setInterval(tick, 1000);

  // ── 로그아웃 → 세션 정리 ───────────────────────────────
  var logout = document.getElementById('logoutBtn');
  if (logout) {
    logout.addEventListener('click', function () {
      if (window.QAGPT) window.QAGPT.clearUser();
    });
  }

  // ── 알림 배지 (승인 결과 등) ───────────────────────────
  function refreshBell(){
    var el = document.getElementById('bellCount');
    if (!el || !(window.MOCK && window.MOCK.unreadCountFor)) return;
    var n = window.MOCK.unreadCountFor(user.name);
    if (n > 0) { el.textContent = n > 99 ? '99+' : n; el.style.display = 'inline-block'; }
    else { el.textContent = ''; el.style.display = 'none'; }
  }
  refreshBell();
  window.QAGPT_BELL = { refresh: refreshBell };

  // ── 역할 기반 요소 제어 (data-role) ────────────────────
  //   data-role="PM,QA"          → 해당 역할만 노출(그 외 숨김)
  //   data-role-mode="disable"   → 숨기지 않고 비활성화(회색)
  function applyRoleGates(root){
    var nodes = (root || document).querySelectorAll('[data-role]');
    Array.prototype.forEach.call(nodes, function (el) {
      var allow = el.getAttribute('data-role').split(',').map(function (s){ return s.trim(); });
      if (allow.indexOf(ROLE) !== -1) return;             // 허용 역할 → 그대로
      if (el.getAttribute('data-role-mode') === 'disable') {
        el.setAttribute('disabled', 'disabled');
        el.classList.add('is-disabled');
        el.title = '현재 역할(' + ROLE + ')에서는 사용할 수 없습니다.';
      } else {
        el.style.display = 'none';                        // 기본 → 숨김
      }
    });
  }

  // ── 조회 전용 역할 배너 ────────────────────────────────
  function readonlyBanner(){
    if (READONLY_ROLES.indexOf(ROLE) === -1) return;
    var m = document.querySelector('.main');
    if (!m) return;
    var b = document.createElement('div');
    b.className = 'note';
    b.style.marginBottom = '12px';
    b.innerHTML = '🔒 <b>' + esc(ROLE) + '</b> 역할은 <b>조회 전용</b>입니다. 등록·검증·승인 등 편집 기능은 비활성화됩니다.';
    var head = m.querySelector('.page-head');
    if (head && head.nextSibling) m.insertBefore(b, head.nextSibling); else m.insertBefore(b, m.firstChild);
  }

  // ── 페이지 접근 가드 (URL 직접 진입 차단) ──────────────
  //   콘텐츠를 삭제하지 않고 숨김 → 페이지 스크립트가 참조하는 노드는 유지(오류 방지)
  function accessGuard(){
    // 상단 대분류 → 사이드 그룹 → 항목(data-page 매칭) 순으로 모두 만족해야 접근 허용
    var denied = !can(active.roles) ? (active.roles || []) : null;
    if (!denied && pageKey) {
      (active.side || []).forEach(function (grp){
        (grp.items || []).forEach(function (it){
          if (it.p !== pageKey) return;
          if (!can(grp.roles)) denied = grp.roles || [];
          else if (!can(it.roles)) denied = it.roles || [];
        });
      });
    }
    if (!denied) return true;
    var m = document.querySelector('.main');
    if (!m) return false;
    Array.prototype.forEach.call(m.children, function (c){ c.style.display = 'none'; });
    var box = document.createElement('div');
    box.innerHTML =
      '<div class="page-head"><div><h1>접근 권한이 없습니다</h1>' +
      '<p>현재 역할(<b>' + esc(ROLE) + '</b>)은 이 화면을 열 수 없습니다.</p></div></div>' +
      '<section class="panel"><div class="p-body">' +
      '이 메뉴는 <b>' + esc(denied.join(', ')) + '</b> 역할에게만 제공됩니다. ' +
      '<a class="btn sm" href="dashboard.html" style="margin-left:8px">나의 현황으로</a>' +
      '</div></section>';
    m.insertBefore(box, m.firstChild);
    return false;
  }

  if (accessGuard()) {          // 접근 허용일 때만 배너·버튼 게이트 적용
    readonlyBanner();
    applyRoleGates();
    setTimeout(function(){ applyRoleGates(); }, 0);   // 동적 렌더 요소 대비
  }

  // 외부에서 재적용할 수 있도록 노출
  window.QAGPT_LAYOUT = { role: ROLE, applyRoleGates: applyRoleGates, can: can };
})();
