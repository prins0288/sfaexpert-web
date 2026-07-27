/* StarSFA admin shell + Gmail-style multi-account (/u/N).
   Each page provides <div id="app" data-page=".." data-title=".."></div> and a
   <template id="pageContent">...</template>; this injects the shell, wires the
   account switcher, prefixes in-app links with /u/{index}/, then calls
   window.pageInit() if defined. */
(function () {
  const KEY = 'sfa_accounts';

  function listAccounts() {
    try { return JSON.parse(localStorage.getItem(KEY)) || []; } catch (e) { return []; }
  }
  function decodeJwt(t) {
    try {
      const p = t.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      return JSON.parse(decodeURIComponent(escape(atob(p))));
    } catch (e) { return {}; }
  }
  function activeIndexFromUrl() {
    const m = location.pathname.match(/^\/u\/(\d+)(\/|$)/);
    return m ? parseInt(m[1], 10) : 0;
  }
  function currentRest() {
    const m = location.pathname.match(/^\/u\/\d+\/(.*)$/);
    return m && m[1] ? m[1] : 'dashboard.html';
  }
  function uUrl(i, rest) { return '/u/' + i + '/' + String(rest || '').replace(/^\//, ''); }

  const accounts = listAccounts();
  const u = activeIndexFromUrl();
  const acc = accounts[u];
  // No such account in this browser -> back to login.
  if (!acc || !acc.token) { location.replace('/'); return; }

  const token = acc.token;
  const claims = decodeJwt(token);
  const user = acc.user || claims.sub || 'user';
  const tenant = acc.tenant || claims.tenant || '?';

  const esc = s => (s == null ? '' : String(s)
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;'));

  const NAV = [
    { group: null, items: [
      { page: 'dashboard', href: 'dashboard.html', icon: 'speedometer2', label: 'Dashboard' } ] },
    { group: 'Masters', items: [
      { page: 'employee',   href: 'master/employee.html',    icon: 'person-badge', label: 'Employee' },
      { page: 'route',      href: 'master/route.html',       icon: 'signpost-2', label: 'Route' },
      { page: 'area',       href: 'master/area.html',        icon: 'geo-alt',    label: 'Area' },
      { page: 'routearea',  href: 'master/route-area.html',  icon: 'diagram-3',  label: 'Route-Area' },
      { page: 'clienttype', href: 'master/client-type.html', icon: 'tags',       label: 'Client Type' },
      { page: 'client',     href: 'master/client.html',      icon: 'people',     label: 'Client' },
      { page: 'products',   href: 'master/products.html',    icon: 'box-seam',   label: 'Products' } ] }
  ];

  const avatarColors = ['#2a56a0', '#0aa3a3', '#1a9d63', '#b0384a', '#7c3aed', '#c2410c'];
  const initial = s => (s ? s.trim().charAt(0).toUpperCase() : '?');

  window.SFA = {
    token, claims, user, tenant, u, esc, accounts, role: claims.role,
    me() { return this.api('/api/me'); },   // full profile (common + tenant employee)
    money: n => (n == null || n === '' ? '' : '₹ ' + Number(n).toLocaleString('en-IN', { minimumFractionDigits: 2 })),
    statusBadge: st => st === 'Y'
      ? '<span class="badge badge-active">Active</span>'
      : '<span class="badge badge-inactive">Inactive</span>',
    fillSelect(sel, items, placeholder) {
      sel.innerHTML = '<option value="">' + (placeholder || '--') + '</option>' +
        (items || []).map(i => `<option value="${i.oid}">${esc(i.label)}</option>`).join('');
    },
    async api(path, opts) {
      opts = opts || {};
      opts.headers = Object.assign({ 'Authorization': 'Bearer ' + token }, opts.headers || {});
      if (opts.body && typeof opts.body !== 'string' && !(opts.body instanceof FormData)) {
        opts.headers['Content-Type'] = 'application/json';
        opts.body = JSON.stringify(opts.body);
      }
      const res = await fetch(path, opts);
      if (res.status === 401) { signOut(u); throw new Error('unauthorized'); }
      const data = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(data.message || data.error || ('HTTP ' + res.status));
      return data;
    },
    async download(path, filename) {
      const res = await fetch(path, { headers: { 'Authorization': 'Bearer ' + token } });
      if (res.status === 401) { signOut(u); return; }
      if (!res.ok) throw new Error('HTTP ' + res.status);
      const blob = await res.blob();
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url; a.download = filename || 'download';
      document.body.appendChild(a); a.click(); a.remove();
      URL.revokeObjectURL(url);
    },
    toast(msg, ok) {
      const el = document.getElementById('sfaMsg');
      if (!el) { return; }
      el.className = 'alert py-2 small ' + (ok === false ? 'alert-danger' : 'alert-success');
      el.textContent = msg;
      el.style.display = 'block';
      setTimeout(() => { el.style.display = 'none'; }, ok === false ? 8000 : 3000);
    }
  };

  function signOut(index) {
    const arr = listAccounts();
    arr.splice(index, 1);
    localStorage.setItem(KEY, JSON.stringify(arr));
    if (arr.length) { location.replace(uUrl(0, 'dashboard.html')); }
    else { localStorage.removeItem(KEY); location.replace('/'); }
  }
  function signOutAll() { localStorage.removeItem(KEY); location.replace('/'); }

  function accountMenu() {
    const rest = currentRest();
    const items = accounts.map((a, i) => {
      const active = i === u;
      const color = avatarColors[i % avatarColors.length];
      const role = (decodeJwt(a.token).role) || '';
      return `
        <a class="dropdown-item d-flex align-items-center gap-2 ${active ? 'active' : ''}" href="${uUrl(i, rest)}">
          <span class="sfa-avatar" style="background:${color}">${esc(initial(a.user))}</span>
          <span class="flex-grow-1">
            <span class="d-block fw-semibold">${esc(a.user)}${active ? ' &check;' : ''}
              ${role ? `<span class="badge bg-secondary" style="font-size:10px">${esc(role)}</span>` : ''}</span>
            <small class="text-muted">tenant: ${esc(a.tenant)} &middot; /u/${i}</small>
          </span>
        </a>`;
    }).join('');
    return `
      ${items}
      <div class="dropdown-divider"></div>
      <a class="dropdown-item" href="/?add=1"><i class="bi bi-person-plus"></i> Add another account</a>
      <a class="dropdown-item" href="#" id="signOutThis"><i class="bi bi-box-arrow-right"></i> Sign out <b>${esc(user)}</b></a>
      <a class="dropdown-item text-danger" href="#" id="signOutAll"><i class="bi bi-power"></i> Sign out all accounts</a>`;
  }

  function rewriteLinks() {
    // Prefix in-app absolute links with /u/{u}/ so navigation keeps the account.
    document.querySelectorAll('a[href^="/"]').forEach(a => {
      const h = a.getAttribute('href');
      if (h === '/' || h.startsWith('/?') || /^\/(u\/|api\/|css\/|js\/|favicon)/.test(h)) return;
      a.setAttribute('href', uUrl(u, h));
    });
  }

  document.addEventListener('DOMContentLoaded', function () {
    const app = document.getElementById('app');
    const page = app.getAttribute('data-page');
    const title = app.getAttribute('data-title') || '';
    const tpl = document.getElementById('pageContent');

    const sidebar = NAV.map(sec => {
      const grp = sec.group ? `<div class="sfa-nav-group">${sec.group}</div>` : '';
      const links = sec.items.map(it =>
        `<a href="${uUrl(u, it.href)}" class="${it.page === page ? 'active' : ''}"><i class="bi bi-${it.icon}"></i><span>${it.label}</span></a>`
      ).join('');
      return grp + links;
    }).join('');

    const color = avatarColors[u % avatarColors.length];

    app.className = 'sfa-shell';
    app.innerHTML = `
      <aside class="sfa-sidebar">
        <div class="sfa-brand"><i class="bi bi-graph-up-arrow"></i><span>StarSFA</span></div>
        <nav class="sfa-nav">${sidebar}</nav>
      </aside>
      <div class="sfa-main">
        <header class="sfa-topbar">
          <button class="btn btn-sm sfa-toggle" id="sfaToggle"><i class="bi bi-list"></i></button>
          <div class="sfa-title">${esc(title)}</div>
          <div class="sfa-user dropdown">
            <a href="#" class="dropdown-toggle d-flex align-items-center gap-2" data-bs-toggle="dropdown">
              <span class="sfa-avatar" style="background:${color}">${esc(initial(user))}</span>
              <span class="d-none d-md-inline">${esc(user)}
                ${claims.role ? `<span class="badge bg-secondary" style="font-size:10px">${esc(claims.role)}</span>` : ''}
                <small class="text-muted">(${esc(tenant)} &middot; /u/${u})</small></span>
            </a>
            <ul class="dropdown-menu dropdown-menu-end" style="min-width:260px">${accountMenu()}</ul>
          </div>
        </header>
        <main class="sfa-content">
          <div id="sfaMsg" class="alert py-2 small" style="display:none"></div>
          <div id="sfaContent"></div>
        </main>
      </div>`;

    if (tpl) { document.getElementById('sfaContent').appendChild(tpl.content.cloneNode(true)); }
    rewriteLinks();
    document.getElementById('signOutThis').addEventListener('click', e => { e.preventDefault(); signOut(u); });
    document.getElementById('signOutAll').addEventListener('click', e => { e.preventDefault(); signOutAll(); });
    if (window.pageInit) { window.pageInit(); }
  });
})();
