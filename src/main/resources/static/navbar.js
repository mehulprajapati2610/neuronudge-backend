// navbar.js — NeuroNudge shared navigation (uses sessionStorage)

function renderNavbar(activePage = '') {
  const role = sessionStorage.getItem('role') || 'USER';
  const name = sessionStorage.getItem('name') || 'User';
  const initials = name.split(' ').map(n => n[0]).join('').slice(0,2).toUpperCase();

  const userLinks = [
      { href:'user-dashboard.html', label:'Dashboard',    id:'dashboard'    },
      { href:'insights.html',       label:'Insights',     id:'insights'     },
      { href:'nudges.html',         label:'Nudges',       id:'nudges'       },
      { href:'chat.html',           label:'Chat',         id:'chat'         },
      { href:'peer.html',           label:'Peer Support', id:'peer'         },
      { href:'assessment.html',     label:'Assessment',   id:'assessment'   },
      { href:'appointments.html',   label:'Appointments', id:'appointments' },
      { href:'my-reports.html',     label:'My Reports',   id:'my-reports'   },
    ];
  const doctorLinks = [
    { href:'doctor-dashboard.html',    label:'Dashboard',    id:'dashboard'    },
    { href:'patients.html',            label:'Patients',     id:'patients'     },
    { href:'doctor-appointments.html', label:'Appointments', id:'appointments' },
  ];
const adminLinks = [
    { href:'admin-dashboard.html', label:'Dashboard', id:'dashboard' },
  ];
  const links = role === 'DOCTOR' ? doctorLinks : role === 'ADMIN' ? adminLinks : userLinks;
  document.body.insertAdjacentHTML('afterbegin', `
  <nav id="main-nav">
    <style>
      #main-nav{position:fixed;top:0;left:0;right:0;z-index:100;background:rgba(22,27,39,0.95);backdrop-filter:blur(16px);border-bottom:1px solid rgba(42,51,71,0.8);height:60px;display:flex;align-items:center;padding:0 24px;font-family:'DM Sans',sans-serif;}
      .nav-logo{display:flex;align-items:center;gap:9px;text-decoration:none;color:#e2e8f0;font-size:0.95rem;font-weight:600;letter-spacing:-0.02em;margin-right:28px;flex-shrink:0;}
      .nav-logo-icon{width:30px;height:30px;background:linear-gradient(135deg,#4f8ef7,#2dd4bf);border-radius:8px;display:flex;align-items:center;justify-content:center;}
      .nav-links{display:flex;align-items:center;gap:2px;flex:1;}
      .nav-link{display:flex;align-items:center;padding:7px 12px;border-radius:8px;text-decoration:none;color:#8899b4;font-size:0.85rem;font-weight:500;transition:all 0.15s;}
      .nav-link:hover{color:#e2e8f0;background:rgba(42,51,71,0.6);}
      .nav-link.active{color:#e2e8f0;background:rgba(79,142,247,0.12);}
      .nav-right{display:flex;align-items:center;gap:8px;margin-left:auto;}
      .notif-btn{position:relative;width:34px;height:34px;border-radius:8px;background:rgba(42,51,71,0.6);border:1px solid #2a3347;color:#8899b4;cursor:pointer;display:flex;align-items:center;justify-content:center;transition:all 0.15s;}
      .notif-btn:hover{color:#e2e8f0;background:rgba(42,51,71,1);}
      .notif-badge{position:absolute;top:5px;right:5px;min-width:14px;height:14px;background:#ef4444;border-radius:99px;border:2px solid #161b27;font-size:9px;font-weight:700;color:#fff;display:flex;align-items:center;justify-content:center;padding:0 2px;display:none;}
      .notif-dropdown{position:absolute;top:calc(100% + 8px);right:0;width:320px;background:#1e2535;border:1px solid #2a3347;border-radius:14px;box-shadow:0 8px 32px rgba(0,0,0,0.5);z-index:300;display:none;overflow:hidden;}
      .notif-dropdown.open{display:block;}
      .notif-header{padding:14px 16px;border-bottom:1px solid #2a3347;display:flex;align-items:center;justify-content:space-between;}
      .notif-title{font-size:0.85rem;font-weight:600;color:#e2e8f0;}
      .notif-mark-all{font-size:0.75rem;color:#4f8ef7;cursor:pointer;background:none;border:none;font-family:'DM Sans',sans-serif;}
      .notif-mark-all:hover{text-decoration:underline;}
      .notif-list{max-height:360px;overflow-y:auto;}
      .notif-item{display:flex;gap:10px;padding:12px 16px;border-bottom:1px solid rgba(42,51,71,0.5);cursor:pointer;transition:background 0.15s;}
      .notif-item:hover{background:rgba(42,51,71,0.5);}
      .notif-item.unread{background:rgba(79,142,247,0.05);}
      .notif-item:last-child{border-bottom:none;}
      .notif-icon{width:32px;height:32px;border-radius:8px;display:flex;align-items:center;justify-content:center;font-size:0.9rem;flex-shrink:0;}
      .notif-body{flex:1;min-width:0;}
      .notif-item-title{font-size:0.82rem;font-weight:500;color:#e2e8f0;margin-bottom:2px;}
      .notif-item-msg{font-size:0.75rem;color:#8899b4;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;}
      .notif-time{font-size:0.7rem;color:#8899b4;flex-shrink:0;margin-top:2px;}
      .notif-unread-dot{width:6px;height:6px;border-radius:50%;background:#4f8ef7;flex-shrink:0;margin-top:6px;}
      .profile-btn{display:flex;align-items:center;gap:8px;padding:5px 10px 5px 5px;border-radius:8px;cursor:pointer;background:rgba(42,51,71,0.4);border:1px solid #2a3347;transition:all 0.15s;position:relative;}
      .profile-btn:hover{background:rgba(42,51,71,0.8);}
      .avatar{width:28px;height:28px;border-radius:7px;background:linear-gradient(135deg,#4f8ef7,#2dd4bf);display:flex;align-items:center;justify-content:center;font-size:0.7rem;font-weight:700;color:white;}
      .profile-name{font-size:0.82rem;font-weight:500;color:#e2e8f0;}
      .profile-dd{position:absolute;top:calc(100% + 8px);right:0;background:#1e2535;border:1px solid #2a3347;border-radius:12px;padding:6px;min-width:160px;display:none;box-shadow:0 8px 32px rgba(0,0,0,0.4);z-index:200;}
      .profile-dd.open{display:block;}
      .dd-item{display:flex;align-items:center;gap:8px;padding:8px 12px;border-radius:8px;font-size:0.85rem;color:#8899b4;cursor:pointer;transition:all 0.15s;text-decoration:none;}
      .dd-item:hover{color:#e2e8f0;background:rgba(42,51,71,0.8);}
      .dd-sep{height:1px;background:#2a3347;margin:4px 0;}
      .dd-logout{color:#f87171!important;}
      @keyframes pulse{0%,100%{opacity:1;}50%{opacity:0.4;}}
    </style>
    <a class="nav-logo" href="${role==='DOCTOR'?'doctor-dashboard.html':'user-dashboard.html'}">
      <div class="nav-logo-icon"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.5"><path d="M12 2a10 10 0 1 0 10 10"/><path d="M12 6v6l4 2"/><circle cx="18" cy="6" r="3" fill="rgba(255,255,255,0.3)" stroke="white" stroke-width="2"/></svg></div>
      NeuroNudge
    </a>
    <div class="nav-links">
      ${links.map(l=>`<a class="nav-link ${activePage===l.id?'active':''}" href="${l.href}">${l.label}</a>`).join('')}
    </div>
    <div class="nav-right">
      <div style="position:relative;">
        <button class="notif-btn" id="notifBtn" onclick="toggleNotifications()">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>
          <span class="notif-badge" id="notifBadge"></span>
        </button>
        <div class="notif-dropdown" id="notifDropdown">
          <div class="notif-header">
            <span class="notif-title">Notifications</span>
            <button class="notif-mark-all" onclick="markAllRead()">Mark all read</button>
          </div>
          <div class="notif-list" id="notifList"><div style="padding:20px;text-align:center;color:#8899b4;font-size:0.85rem;">Loading…</div></div>
        </div>
      </div>
      <div class="profile-btn" onclick="toggleProfileDD()">
        <div class="avatar">${initials}</div>
        <span class="profile-name">${escHtml(name.split(' ')[0])}</span>
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#8899b4" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
        <div class="profile-dd" id="profileDD">
          <a class="dd-item" href="profile.html">⚙ Settings &amp; Profile</a>
          <div class="dd-sep"></div>
          <div class="dd-item dd-logout" onclick="logout()">↪ Sign out</div>
        </div>
      </div>
    </div>
  </nav>`);

  document.body.style.paddingTop = '60px';
  loadNotifCount();
  setInterval(loadNotifCount, 30000);
}

// Notification type → icon + bg color
const NOTIF_ICONS = {
  APPOINTMENT_ACCEPTED: { icon:'✓', bg:'rgba(74,222,128,0.12)' },
  APPOINTMENT_REJECTED: { icon:'✗', bg:'rgba(239,68,68,0.12)' },
  APPOINTMENT_REQUEST:  { icon:'📅', bg:'rgba(79,142,247,0.12)' },
  APPOINTMENT_CANCELLED:{ icon:'✕', bg:'rgba(251,146,60,0.12)' },
  RECOMMENDATION:       { icon:'💡', bg:'rgba(45,212,191,0.12)' },
  BURNOUT_ALERT:        { icon:'⚠', bg:'rgba(249,115,22,0.12)' },
  NUDGE_REMINDER:       { icon:'🌿', bg:'rgba(163,230,53,0.1)' },
  GENERAL:              { icon:'•', bg:'rgba(42,51,71,0.5)' },
};

async function loadNotifCount() {
  try {
    const data = await API.get('/api/notifications/count');
    const badge = document.getElementById('notifBadge');
    if (!badge) return;
    if (data && data.count > 0) {
      badge.textContent = data.count > 9 ? '9+' : data.count;
      badge.style.display = 'flex';
    } else {
      badge.style.display = 'none';
    }
  } catch(e) { /* silent */ }
}

async function loadNotifications() {
  const list = document.getElementById('notifList');
  try {
    const notifs = await API.get('/api/notifications');
    if (!notifs || notifs.length === 0) {
      list.innerHTML = '<div style="padding:32px;text-align:center;color:#8899b4;font-size:0.85rem;">All caught up! 🎉</div>';
      return;
    }
    list.innerHTML = notifs.map(n => {
      const meta = NOTIF_ICONS[n.type] || NOTIF_ICONS.GENERAL;
      return `<div class="notif-item ${n.read?'':'unread'}" onclick="clickNotif('${n.id}','${n.link||''}')">
        <div class="notif-icon" style="background:${meta.bg};">${meta.icon}</div>
        <div class="notif-body">
          <div class="notif-item-title">${escHtml(n.title)}</div>
          <div class="notif-item-msg">${escHtml(n.message)}</div>
        </div>
        <div>
          <div class="notif-time">${relativeTime(n.createdAt)}</div>
          ${n.read ? '' : '<div class="notif-unread-dot"></div>'}
        </div>
      </div>`;
    }).join('');
  } catch(e) {
    list.innerHTML = '<div style="padding:20px;text-align:center;color:#8899b4;font-size:0.85rem;">Could not load</div>';
  }
}

function toggleNotifications() {
  const dd = document.getElementById('notifDropdown');
  const isOpen = dd.classList.toggle('open');
  if (isOpen) loadNotifications();
  document.getElementById('profileDD').classList.remove('open');
}

async function clickNotif(id, link) {
  try { await API.post(`/api/notifications/${id}/read`, {}); } catch(e) {}
  document.getElementById('notifDropdown').classList.remove('open');
  loadNotifCount();
  if (link) window.location.href = link;
}

async function markAllRead() {
  try {
    await API.post('/api/notifications/read-all', {});
    loadNotifCount();
    loadNotifications();
  } catch(e) {}
}

function toggleProfileDD() {
  document.getElementById('profileDD').classList.toggle('open');
  document.getElementById('notifDropdown').classList.remove('open');
}

function logout() { sessionStorage.clear(); window.location.href = 'login.html'; }

document.addEventListener('click', e => {
  if (!e.target.closest('#notifBtn') && !e.target.closest('#notifDropdown')) {
    document.getElementById('notifDropdown')?.classList.remove('open');
  }
  if (!e.target.closest('.profile-btn')) {
    document.getElementById('profileDD')?.classList.remove('open');
  }
});

function escHtml(s) {
  if (!s) return '';
  return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
}
