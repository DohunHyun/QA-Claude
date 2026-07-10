/* ============================================================
   QA-Claude 세션 헬퍼 — 로그인 사용자/계정 정보 저장·조회
   (백엔드 세션 도입 전까지 localStorage 사용. 이후 API 응답으로 교체)
   ============================================================ */
window.QAGPT = (function () {
  var KEY = 'qagpt.user';

  // ── 역할별 계정 (데모용) ────────────────────────────────
  //   pw는 시연 편의를 위한 자리표시자 — 실제 인증은 백엔드가 담당
  var ACCOUNTS = [
    { id: 'kmh',  pw: '1234', name: '김명호', role: 'PM',
      org: 'NH디지털연구소', project: '투자자문 관리시스템(NBIA)' },
    { id: 'park', pw: '1234', name: '박서준', role: 'PM',
      org: 'NH디지털연구소', project: '하나 펀드 판매관리 고도화(HFMS)' },
    { id: 'hdh',  pw: '1234', name: '현도훈', role: '관리자',
      org: 'NH디지털연구소', project: '전체 프로젝트' },
    { id: 'khg',  pw: '1234', name: '김홍규', role: 'QA',
      org: 'NH디지털연구소', project: '투자자문 관리시스템(NBIA)' },
    { id: 'client', pw: '1234', name: '고객담당', role: '고객사',
      org: '(은행) 발주사', project: '투자자문 관리시스템(NBIA)' },
    { id: 'user', pw: '1234', name: '일반사용자', role: '일반',
      org: 'NH디지털연구소', project: '투자자문 관리시스템(NBIA)' }
  ];

  var DEFAULT_USER = ACCOUNTS[0]; // 미로그인 폴백(PM 김명호)

  function getAccounts() { return ACCOUNTS.slice(); }

  function findAccount(id) {
    id = String(id || '').trim().toLowerCase();
    for (var i = 0; i < ACCOUNTS.length; i++) {
      if (ACCOUNTS[i].id.toLowerCase() === id) return ACCOUNTS[i];
    }
    return null;
  }

  // 아이디 → 표시 이름 (없으면 아이디 그대로)
  function nameFromId(id) {
    var a = findAccount(id);
    return a ? a.name : id;
  }

  // 아이디/비밀번호 검증 → 계정(사본) 반환, 실패 시 null
  function authenticate(id, pw) {
    var a = findAccount(id);
    if (!a) return null;
    if (pw != null && String(pw) !== a.pw) return null;
    // 저장용 사용자 객체(비밀번호 제외)
    return { id: a.id, name: a.name, role: a.role, org: a.org, project: a.project };
  }

  function getUser() {
    try {
      var raw = localStorage.getItem(KEY);
      if (!raw) return DEFAULT_USER;
      var u = JSON.parse(raw);
      return u && u.name ? u : DEFAULT_USER;
    } catch (e) {
      return DEFAULT_USER;
    }
  }

  function setUser(u) {
    try { localStorage.setItem(KEY, JSON.stringify(u)); } catch (e) {}
  }

  function clearUser() {
    try { localStorage.removeItem(KEY); } catch (e) {}
  }

  return {
    getUser: getUser, setUser: setUser, clearUser: clearUser,
    getAccounts: getAccounts, findAccount: findAccount,
    authenticate: authenticate, nameFromId: nameFromId
  };
})();
