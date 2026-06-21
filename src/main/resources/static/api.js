// api.js — NeuroNudge shared API helper
// Uses sessionStorage (per-tab) to fix multi-tab login conflict

const API = {
  token: ()  => sessionStorage.getItem('token'),
  userId: () => sessionStorage.getItem('userId'),
  role: ()   => sessionStorage.getItem('role'),
  name: ()   => sessionStorage.getItem('name'),

  headers() {
    return {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer ' + this.token()
    };
  },

  async get(path) {
    const res = await fetch(path, { headers: this.headers() });
    if (res.status === 401) { sessionStorage.clear(); window.location.href = 'login.html'; return; }
    if (!res.ok) throw new Error(`GET ${path} → ${res.status}`);
    return res.json();
  },

  async post(path, body) {
    const res = await fetch(path, {
      method: 'POST',
      headers: this.headers(),
      body: JSON.stringify(body)
    });
    if (res.status === 401) { sessionStorage.clear(); window.location.href = 'login.html'; return; }
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || `POST ${path} → ${res.status}`);
    }
    return res.json();
  },

  async put(path, body) {
    const res = await fetch(path, {
      method: 'PUT',
      headers: this.headers(),
      body: JSON.stringify(body)
    });
    if (res.status === 401) { sessionStorage.clear(); window.location.href = 'login.html'; return; }
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || `PUT ${path} → ${res.status}`);
    }
    return res.json();
  },

  async delete(path) {
    const res = await fetch(path, {
      method: 'DELETE',
      headers: this.headers()
    });
    if (res.status === 401) { sessionStorage.clear(); window.location.href = 'login.html'; return; }
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || `DELETE ${path} → ${res.status}`);
    }
    return res.json();
  }
};

function skeleton(lines = 3) {
  return Array.from({ length: lines }, () =>
    `<div style="height:14px;background:var(--surface2);border-radius:6px;margin-bottom:10px;animation:pulse 1.5s ease-in-out infinite;"></div>`
  ).join('');
}

function emptyState(icon, msg, sub = '') {
  return `<div style="text-align:center;padding:48px 20px;color:var(--muted);">
    <div style="font-size:2rem;margin-bottom:10px;">${icon}</div>
    <div style="font-size:0.92rem;font-weight:500;color:var(--text);margin-bottom:4px;">${escHtml(msg)}</div>
    ${sub ? `<div style="font-size:0.8rem;">${escHtml(sub)}</div>` : ''}
  </div>`;
}

function relativeTime(dateStr) {
  if (!dateStr) return '';
  const diff = Date.now() - new Date(dateStr).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1)  return 'Just now';
  if (mins < 60) return `${mins}m ago`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24)  return `${hrs}h ago`;
  const days = Math.floor(hrs / 24);
  if (days === 1) return 'Yesterday';
  if (days < 7)  return `${days} days ago`;
  return new Date(dateStr).toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
}

function fmtDate(d) {
  if (!d) return '—';
  return new Date(d + (d.length === 10 ? 'T00:00:00' : '')).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
}

function escHtml(s) {
  if (!s) return '';
  return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;').replace(/'/g,'&#x27;');
}

function cap(s) { return s ? s.charAt(0).toUpperCase() + s.slice(1).toLowerCase() : ''; }

function requireAuth() {
  if (!sessionStorage.getItem('token')) {
    window.location.href = 'login.html';
  }
}
