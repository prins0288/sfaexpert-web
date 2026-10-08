/* ============================================================================
 * StarSFA application shell.
 *
 * Builds the whole chrome around each page's <template id="pageContent">:
 *   - navigation is DB-driven (GET /api/menu): a 3-level, role-filtered tree
 *     rendered as a VERTICAL sidebar or HORIZONTAL menu bar, with per-item
 *     icon/logo and a Favorites group the user curates with the star toggle
 *   - a dynamic breadcrumb trail for the current page
 *   - header (title, nav search, dark/light toggle, appearance customizer,
 *     Gmail-style multi-account switcher) and footer — both frozen, only the
 *     content area scrolls
 *
 * Data/session/theme come from the core modules (Session, Api, Theme, API);
 * the shell assembles DOM and wires events.
 * ==========================================================================*/
(function (window) {
  "use strict";

  const account = Session.current();
  if (!account || !account.token) { location.replace(Session.ctx() + "/"); return; }

  const claims = Session.claims();
  const user = account.user || claims.username || claims.sub || "user";
  const companyCode = account.companyCode || claims.companyCode || "?";
  const role = claims.role || "";
  const u = Session.activeIndex();

  const esc = (s) => (s == null ? "" : String(s)
    .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;"));

  /** dd-MM-yyyy HH:mm:ss, e.g. "30-07-2026 15:30:34". Accepts a Date, an ISO string, or null. */
  function fmtDateTime(d) {
    if (!d) return "";
    const dt = d instanceof Date ? d : new Date(d);
    if (isNaN(dt.getTime())) return "";
    const p = (n) => String(n).padStart(2, "0");
    return `${p(dt.getDate())}-${p(dt.getMonth() + 1)}-${dt.getFullYear()} ${p(dt.getHours())}:${p(dt.getMinutes())}:${p(dt.getSeconds())}`;
  }

  // Menu state (filled from the server; DEFAULT_MENU is the offline fallback).
  let MENU = [];
  let FAV = new Set();

  // Fallback tree (ids null => no favorites), used only if /api/menu fails.
  const DEFAULT_MENU = [
    { id: null, label: "Dashboard", icon: "speedometer2", page: "dashboard", href: "dashboard.html", children: [] },
    { id: null, label: "Masters", icon: "collection", children: [
      { id: null, label: "Geography", icon: "geo", children: [
        { id: null, label: "Area", icon: "geo-alt", page: "area", href: "master/area.html", children: [] },
        { id: null, label: "Route", icon: "signpost-2", page: "route", href: "master/route.html", children: [] },
        { id: null, label: "Route-Area Map", icon: "diagram-3", page: "routearea", href: "master/route-area.html", children: [] } ] },
      { id: null, label: "Parties", icon: "people", children: [
        { id: null, label: "Client Type", icon: "tags", page: "clienttype", href: "master/client-type.html", children: [] },
        { id: null, label: "Client", icon: "person-vcard", page: "client", href: "master/client.html", children: [] } ] },
      { id: null, label: "Catalog", icon: "box-seam", children: [
        { id: null, label: "Products", icon: "boxes", page: "products", href: "master/products.html", children: [] } ] } ] },
    { id: null, label: "Administration", icon: "shield-lock", children: [
      { id: null, label: "Preferences", icon: "sliders", children: [
        { id: null, label: "Appearance", icon: "palette", page: "appearance", href: "settings/appearance.html", children: [] } ] } ] },
  ];

  /** Apply a fetched-or-cached /api/menu response to MENU/FAV. */
  function applyMenuResponse(res) {
    MENU = (res && res.items) || [];
    FAV = new Set((res && res.favorites) || []);
    if (!MENU.length) MENU = DEFAULT_MENU;
  }

  async function loadMenu() {
    // Same-tab, same-account cache (Session.cache) — skips the network call
    // entirely on every page navigation after the first. See theme.js's load()
    // for the full rationale; refreshNav()'s favourite toggle keeps this fresh.
    const cached = Session.cache.get("menu");
    if (cached) { applyMenuResponse(cached); return; }
    try {
      const res = await Api.get(API.menu.get);
      applyMenuResponse(res);
      Session.cache.set("menu", res);
      try { localStorage.setItem("sfa_menu", JSON.stringify(res)); } catch (e) {}
    } catch (e) {
      // offline / not migrated yet: try the older cross-session cache, then the built-in default
      try {
        const c = JSON.parse(localStorage.getItem("sfa_menu"));
        MENU = (c && c.items && c.items.length) ? c.items : DEFAULT_MENU;
        FAV = new Set((c && c.favorites) || []);
      } catch (e2) { MENU = DEFAULT_MENU; FAV = new Set(); }
    }
  }

  // ---- permissions: what this user may do (GET /api/permissions/my) -------
  // { CODE: true|false }. A code missing from the map counts as allowed —
  // same default as the server (no permission_assignment row = full access).
  // This only hides buttons; the server still enforces @RequiresPermission.
  let PERMS = {};

  async function loadPermissions() {
    const cached = Session.cache.get("perms");
    if (cached) { PERMS = cached; return; }
    try {
      PERMS = (await Api.get(API.permissions.my, { noLoader: true })) || {};
      Session.cache.set("perms", PERMS);
    } catch (e) { PERMS = {}; }   // not migrated yet / offline -> show everything, server decides
  }

  const can = (code) => !code || PERMS[code] !== false;

  /** Hide every [data-perm="CODE"] element (inside root) the user isn't allowed. */
  function applyPermissions(root) {
    const scope = root && root.querySelectorAll ? root : document;
    const els = scope.querySelectorAll("[data-perm]");
    els.forEach((el) => { el.hidden = !can(el.getAttribute("data-perm")); });
    if (scope !== document && scope.hasAttribute && scope.hasAttribute("data-perm")) {
      scope.hidden = !can(scope.getAttribute("data-perm"));
    }
  }

  // Table rows / modals are rendered after boot (reload() after every save),
  // so watch the DOM and hide newly added [data-perm] buttons as they appear.
  function watchPermissions() {
    applyPermissions(document);
    new MutationObserver((muts) => {
      muts.forEach((m) => m.addedNodes.forEach((n) => { if (n.nodeType === 1) applyPermissions(n); }));
    }).observe(document.body, { childList: true, subtree: true });
  }

  const activePage = () => (document.getElementById("app").getAttribute("data-page") || "");
  const isLeaf = (n) => !n.children || !n.children.length;
  const hasHref = (n) => n.href && String(n.href).trim() !== "";

  function isTrail(node) {
    if (node.page && node.page === activePage()) return true;
    return (node.children || []).some(isTrail);
  }

  function iconMarkup(n, big) {
    if (n.logo) return `<img class="sfa-nav-logo" src="${esc(n.logo)}" alt="" />`;
    if (n.icon) return `<i class="bi bi-${esc(n.icon)}"></i>`;
    return `<i class="bi bi-dot"></i>`;
  }

  function starMarkup(n) {
    if (n.id == null || !hasHref(n)) return "";   // only real leaves are favouritable
    const on = FAV.has(n.id);
    return `<button type="button" class="sfa-fav-btn ${on ? "on" : ""}" data-fav="${n.id}"
      title="${on ? "Remove from favorites" : "Add to favorites"}"><i class="bi bi-star${on ? "-fill" : ""}"></i></button>`;
  }

  // ---- flatten leaves (for search + favorites lookup) --------------------
  function leaves(nodes, trail, out) {
    (nodes || []).forEach((n) => {
      const t = trail.concat(n.label);
      if (hasHref(n)) out.push({ id: n.id, label: n.label, path: t.join(" › "), href: n.href, icon: n.icon, logo: n.logo, linkTarget: n.linkTarget });
      if (n.children) leaves(n.children, t, out);
    });
    return out;
  }
  const favoriteLeaves = () => leaves(MENU, [], []).filter((l) => l.id != null && FAV.has(l.id));

  // ---- breadcrumb trail to the active page -------------------------------
  function trailTo(nodes, path) {
    for (const n of nodes || []) {
      const here = path.concat(n);
      if (n.page && n.page === activePage()) return here;
      if (n.children && n.children.length) {
        const found = trailTo(n.children, here);
        if (found) return found;
      }
    }
    return null;
  }

  function breadcrumbHtml() {
    const crumbs = trailTo(MENU, []) || [];
    const home = `<a href="${Session.uUrl(u, "dashboard.html")}" class="sfa-crumb-home" title="Dashboard"><i class="bi bi-house-door"></i></a>`;
    const parts = crumbs.map((c, i) => {
      const last = i === crumbs.length - 1;
      const label = esc(c.label);
      if (last || !hasHref(c)) return `<span class="sfa-crumb ${last ? "current" : ""}">${label}</span>`;
      return `<a class="sfa-crumb" href="${Session.uUrl(u, c.href)}">${label}</a>`;
    });
    return home + parts.map((p) => `<i class="bi bi-chevron-right sfa-crumb-sep"></i>${p}`).join("");
  }

  /**
   * The report/page title. Sourced from the MENU TABLE (the active item's
   * `title`, falling back to its `label`), so editing the row in menu_item
   * changes the title everywhere. `data-title` on the page is only the last
   * resort for a page that has no matching menu row.
   */
  function pageTitle() {
    const crumbs = trailTo(MENU, []) || [];
    if (crumbs.length) {
      const cur = crumbs[crumbs.length - 1];
      if (cur.title && String(cur.title).trim()) return cur.title;
      if (cur.label) return cur.label;
    }
    return document.getElementById("app").getAttribute("data-title") || "";
  }

  /**
   * Help text for the active page's header help button, sourced from the
   * MENU TABLE (menu_item.description). Null/blank => no help button at all.
   * HTML tags in the value are rendered as-is (the tooltip is initialised
   * with `html: true`), so this is intentionally NOT escaped.
   */
  function pageDescription() {
    const crumbs = trailTo(MENU, []) || [];
    if (!crumbs.length) return "";
    const d = crumbs[crumbs.length - 1].description;
    return d && String(d).trim() ? d : "";
  }

  /** Breadcrumb bar inner: trail on the left, the page/report title (+ optional help button) on the right (same line). */
  function crumbBarInner() {
    const help = pageDescription()
      ? `<button type="button" class="sfa-help-btn" id="sfaHelpBtn" aria-label="Help"><i class="bi bi-question-circle"></i></button>`
      : "";
    return `<div class="sfa-crumb-trail">${breadcrumbHtml()}</div>` +
           `<div class="sfa-crumb-title-wrap"><div class="sfa-crumb-title">${esc(pageTitle())}</div>${help}</div>`;
  }

  /**
   * (Re-)initialise the header help button's popover — a colourful card
   * (gradient header + white body), not a plain tooltip. HTML-enabled
   * (sanitize: false — description is admin-authored DB content, same trust
   * level as icon/href/logo elsewhere in menu_item), hover/focus trigger.
   */
  function wireHelpTooltip() {
    const btn = document.getElementById("sfaHelpBtn");
    if (!btn) return;
    const existing = bootstrap.Popover.getInstance(btn);
    if (existing) existing.dispose();
    new bootstrap.Popover(btn, {
      title: `<i class="bi bi-info-circle-fill"></i> ${esc(pageTitle())} &middot; Help`,
      content: pageDescription(),
      html: true,
      sanitize: false,
      placement: "bottom",
      trigger: "hover focus",
      customClass: "sfa-help-popover",
    });
  }

  // ---- VERTICAL sidebar tree ---------------------------------------------
  function vNode(n, level) {
    if (n.children && n.children.length) {
      const open = isTrail(n);
      return `<div class="sfa-nav-branch level-${level} ${open ? "open" : ""}">
          <button type="button" class="sfa-nav-toggle" aria-expanded="${open}">
            ${iconMarkup(n)}<span class="sfa-nav-text">${esc(n.label)}</span><i class="bi bi-chevron-down sfa-caret"></i>
          </button>
          <div class="sfa-nav-children" ${open ? "" : "hidden"}>
            ${n.children.map((c) => vNode(c, level + 1)).join("")}
          </div>
        </div>`;
    }
    const active = n.page === activePage();
    return `<div class="sfa-nav-leaf-row">
        <a href="${Session.uUrl(u, n.href)}"${tgt(n)} class="sfa-nav-link level-${level} ${active ? "active" : ""}">
          ${iconMarkup(n)}<span class="sfa-nav-text">${esc(n.label)}</span></a>${starMarkup(n)}
      </div>`;
  }

  // company_menu_self.target (MenuNode.linkTarget enum): "_BLANK" opens the link
  // in a new tab (rel guards the opener).
  function tgt(n) { return n && n.linkTarget === "_BLANK" ? ' target="_blank" rel="noopener noreferrer"' : ""; }

  function verticalNavHtml() {
    const favs = favoriteLeaves();
    let html = "";
    if (favs.length) {
      html += `<div class="sfa-nav-branch level-1 open sfa-fav-group">
          <div class="sfa-nav-fav-head"><i class="bi bi-star-fill"></i><span class="sfa-nav-text">Favorites</span></div>
          <div class="sfa-nav-children">${favs.map((f) => `
            <div class="sfa-nav-leaf-row">
              <a href="${Session.uUrl(u, f.href)}"${tgt(f)} class="sfa-nav-link level-2 ${f.page === activePage() ? "active" : ""}">
                ${iconMarkup(f)}<span class="sfa-nav-text">${esc(f.label)}</span></a>
              <button type="button" class="sfa-fav-btn on" data-fav="${f.id}" title="Remove from favorites"><i class="bi bi-star-fill"></i></button>
            </div>`).join("")}</div>
        </div>`;
    }
    html += MENU.map((n) => vNode(n, 1)).join("");
    return html;
  }

  // ---- HORIZONTAL menu bar (dropdown + flyout) ---------------------------
  function hSub(n) {
    if (n.children && n.children.length) {
      return `<div class="sfa-menu-item has-sub">
          <button type="button" class="sfa-menu-sub-toggle">${iconMarkup(n)}<span>${esc(n.label)}</span><i class="bi bi-chevron-right sfa-caret"></i></button>
          <div class="sfa-menu-flyout">${n.children.map(hSub).join("")}</div>
        </div>`;
    }
    const active = n.page === activePage();
    return `<div class="sfa-menu-leaf-row">
        <a href="${Session.uUrl(u, n.href)}"${tgt(n)} class="sfa-menu-leaf ${active ? "active" : ""}">${iconMarkup(n)}<span>${esc(n.label)}</span></a>${starMarkup(n)}
      </div>`;
  }

  function hTop(n) {
    if (n.children && n.children.length) {
      return `<div class="sfa-menu-top ${isTrail(n) ? "trail" : ""}">
          <button type="button" class="sfa-menu-top-btn">${iconMarkup(n)}<span>${esc(n.label)}</span><i class="bi bi-chevron-down sfa-caret"></i></button>
          <div class="sfa-menu-dropdown">${n.children.map(hSub).join("")}</div>
        </div>`;
    }
    const active = n.page === activePage();
    return `<a href="${Session.uUrl(u, n.href)}"${tgt(n)} class="sfa-menu-top sfa-menu-top-link ${active ? "active" : ""}">${iconMarkup(n)}<span>${esc(n.label)}</span></a>`;
  }

  function horizontalNavHtml() {
    const favs = favoriteLeaves();
    let html = "";
    if (favs.length) {
      html += `<div class="sfa-menu-top">
          <button type="button" class="sfa-menu-top-btn"><i class="bi bi-star-fill"></i><span>Favorites</span><i class="bi bi-chevron-down sfa-caret"></i></button>
          <div class="sfa-menu-dropdown">${favs.map((f) => `
            <div class="sfa-menu-leaf-row">
              <a href="${Session.uUrl(u, f.href)}"${tgt(f)} class="sfa-menu-leaf ${f.page === activePage() ? "active" : ""}">${iconMarkup(f)}<span>${esc(f.label)}</span></a>
              <button type="button" class="sfa-fav-btn on" data-fav="${f.id}" title="Remove from favorites"><i class="bi bi-star-fill"></i></button>
            </div>`).join("")}</div>
        </div>`;
    }
    html += MENU.map(hTop).join("");
    return html;
  }

  const navHtml = () => (currentLayout() === "horizontal" ? horizontalNavHtml() : verticalNavHtml());

  // ---- account switcher ---------------------------------------------------
  const avatarColors = ["#2a56a0", "#0aa3a3", "#1a9d63", "#b0384a", "#7c3aed", "#c2410c"];
  const initial = (s) => (s ? s.trim().charAt(0).toUpperCase() : "?");

  // ---- identity: company/default logo (brand) + user photo (avatar) -------
  function readIdentity() {
    try { return JSON.parse(localStorage.getItem("sfa_identity")) || {}; } catch (e) { return {}; }
  }
  let IDENTITY = readIdentity();

  /** force=true bypasses the session cache (used by Shell.refreshIdentity() after a save). */
  async function loadIdentity(force) {
    if (!force) {
      const cached = Session.cache.get("identity");
      if (cached) { IDENTITY = cached; return; }
    }
    try {
      const [b, p] = await Promise.all([
        Api.get(API.branding.get).catch(() => ({})),
        Api.get(API.profile.get).catch(() => ({})),
      ]);
      IDENTITY = { logo: b.logo || null, companyName: b.companyName || null, photo: (p && p.photo) || null };
      Session.cache.set("identity", IDENTITY);
      try { localStorage.setItem("sfa_identity", JSON.stringify(IDENTITY)); } catch (e) {}
    } catch (e) { /* keep whatever we had */ }
  }

  /** The avatar: the user's photo if set, else a coloured initial. */
  function avatarMarkup(color, name, photo) {
    return photo
      ? `<img class="sfa-avatar" src="${photo}" alt="${esc(initial(name))}" />`
      : `<span class="sfa-avatar" style="background:${color}">${esc(initial(name))}</span>`;
  }

  /** The brand: company/default logo if set, else the StarSFA icon. */
  function brandInner() {
    return IDENTITY.logo
      ? `<img class="sfa-brand-logo" src="${IDENTITY.logo}" alt="" /><span>${esc(IDENTITY.companyName || "StarSFA")}</span>`
      : `<i class="bi bi-graph-up-arrow"></i><span>StarSFA</span>`;
  }

  /** Re-render just the brand + header avatar after a photo/logo change. */
  function applyIdentity() {
    document.querySelectorAll(".sfa-brand").forEach((el) => { el.innerHTML = brandInner(); });
    const ha = document.getElementById("sfaHeaderAvatar");
    if (ha) ha.innerHTML = avatarMarkup(avatarColors[u % avatarColors.length], user, IDENTITY.photo);
  }

  function accountMenu() {
    const rest = Session.currentRest();
    const items = Session.list().map((a, i) => {
      const active = i === u;
      const color = avatarColors[i % avatarColors.length];
      const r = (Session.decode(a.token).role) || "";
      return `<a class="dropdown-item d-flex align-items-center gap-2 ${active ? "active" : ""}" href="${Session.uUrl(i, rest)}">
          <span class="sfa-avatar" style="background:${color}">${esc(initial(a.user))}</span>
          <span class="flex-grow-1"><span class="d-block fw-semibold">${esc(a.user)}${active ? " &check;" : ""}
            ${r ? `<span class="badge bg-secondary" style="font-size:10px">${esc(r)}</span>` : ""}</span>
            <small class="text-muted">company: ${esc(a.companyCode)} &middot; /u/${i}</small></span></a>`;
    }).join("");
    return `${items}
      <div class="dropdown-divider"></div>
      <a class="dropdown-item" href="${Session.ctx()}/?add=1"><i class="bi bi-person-plus"></i> Add another account</a>
      <a class="dropdown-item" href="${Session.uUrl(u, "settings/profile.html")}"><i class="bi bi-person-circle"></i> My Profile</a>
      <a class="dropdown-item" href="${Session.uUrl(u, "settings/appearance.html")}"><i class="bi bi-palette"></i> Appearance</a>
      <a class="dropdown-item" href="${Session.uUrl(u, "help/shortcuts.html")}"><i class="bi bi-keyboard"></i> Keyboard shortcuts</a>
      <a class="dropdown-item" href="${Session.uUrl(u, "settings/loader.html")}"><i class="bi bi-hourglass-split"></i> Page loader</a>
      <a class="dropdown-item" href="${Session.uUrl(u, "settings/empcode.html")}"><i class="bi bi-upc-scan"></i> Employee code format</a>
      <div class="dropdown-divider"></div>
      <a class="dropdown-item" href="${Session.uUrl(u, "ai/chat.html")}"><i class="bi bi-robot"></i> AI Assistant</a>
      <a class="dropdown-item" href="${Session.uUrl(u, "settings/ai.html")}"><i class="bi bi-cpu"></i> AI settings</a>
      <a class="dropdown-item" href="#" id="signOutThis"><i class="bi bi-box-arrow-right"></i> Sign out <b>${esc(user)}</b></a>
      <a class="dropdown-item text-danger" href="#" id="signOutAll"><i class="bi bi-power"></i> Sign out all accounts</a>`;
  }

  function headerActions(color) {
    return `<div class="sfa-actions">
        <div class="sfa-search">
          <i class="bi bi-search"></i>
          <input type="text" id="sfaSearch" placeholder="Search menu…" data-i18n-ph="topbar.searchMenu" autocomplete="off" />
          <div class="sfa-search-results" id="sfaSearchResults" hidden></div>
        </div>
        <div class="sfa-lang dropdown">
          <button class="sfa-icon-btn sfa-lang-btn" id="sfaLangBtn" title="Language" data-i18n-title="topbar.language"
                  data-bs-toggle="dropdown" aria-expanded="false"><span id="sfaLangFlag">🌐</span></button>
          <ul class="dropdown-menu dropdown-menu-end" id="sfaLangMenu"></ul>
        </div>
        <button class="sfa-icon-btn" id="sfaModeToggle" title="Toggle light / dark" data-i18n-title="topbar.toggleTheme"><i class="bi bi-circle-half"></i></button>
        <button class="sfa-icon-btn" id="sfaCustomize" title="Customize appearance" data-i18n-title="topbar.appearance"><i class="bi bi-palette"></i></button>
        <div class="sfa-user dropdown">
          <a href="#" class="dropdown-toggle d-flex align-items-center gap-2" data-bs-toggle="dropdown">
            <span id="sfaHeaderAvatar">${avatarMarkup(color, user, IDENTITY.photo)}</span>
            <span class="d-none d-md-inline">${esc(user)}
              ${role ? `<span class="badge bg-secondary" style="font-size:10px">${esc(role)}</span>` : ""}
              <small class="text-muted">(${esc(companyCode)} &middot; /u/${u})</small></span>
          </a>
          <ul class="dropdown-menu dropdown-menu-end" style="min-width:260px">${accountMenu()}</ul>
        </div>
      </div>`;
  }

  // Saved preference vs the layout actually rendered. On narrow screens we always
  // use the touch-friendly vertical drawer, because horizontal hover-dropdowns
  // don't work on touch (same idea as a Bootstrap navbar collapsing on mobile).
  const NAV_BREAKPOINT = 992;
  function isNarrow() { return window.innerWidth <= NAV_BREAKPOINT; }
  function savedLayout() {
    // "_u<account>"-suffixed: the theme is per tenant and several tenants can be
    // signed in at once (see js/core/theme.js).
    const m = location.pathname.match(/^\/u\/(\d+)(\/|$)/);
    return (window.Theme && Theme.state && Theme.state.navLayout) ||
      localStorage.getItem("sfa_nav_u" + (m ? m[1] : "0")) || "vertical";
  }
  function currentLayout() { return isNarrow() ? "vertical" : savedLayout(); }

  // ------------------------------------------------------------------------
  function build() {
    const app = document.getElementById("app");
    const title = pageTitle();          // from the menu table (see pageTitle)
    const layout = currentLayout();
    const color = avatarColors[u % avatarColors.length];
    const brand = `<div class="sfa-brand">${brandInner()}</div>`;
    const crumb = `<nav class="sfa-crumbbar" id="sfaCrumb">${crumbBarInner()}</nav>`;
    const lastLoginText = account && account.lastLoginAt ? fmtDateTime(account.lastLoginAt) : "first login";
    const footer = `<footer class="sfa-footer">
        <span>&copy; ${new Date().getFullYear()} StarSFA · Sales Force Automation
          &middot; <span class="sfa-net-status" id="sfaNetStatus" title="Connection status">
            <i class="bi bi-circle-fill sfa-net-dot"></i> <span class="sfa-net-text" data-i18n="footer.online">Online</span></span></span>
        <span class="sfa-footer-right"><span data-i18n="footer.company">Company:</span> <b>${esc(companyCode)}</b> · <span data-i18n="footer.signedInAs">signed in as:</span> <b>${esc(user)}</b>
          · <span data-i18n="footer.lastLogin">last login:</span> <b>${esc(lastLoginText)}</b> · <b id="sfaClockNow">${esc(fmtDateTime(new Date()))}</b></span>
      </footer>`;
    const content = `<main class="sfa-content"><div id="sfaMsg" class="alert py-2 small" style="display:none"></div><div id="sfaContent"></div></main>`;

    app.className = "sfa-shell";
    app.setAttribute("data-layout", layout);

    if (layout === "horizontal") {
      app.innerHTML = `
        <div class="sfa-main sfa-main-h">
          <header class="sfa-topbar">${brand}<div class="sfa-title">${esc(title)}</div>${headerActions(color)}</header>
          <nav class="sfa-menubar" id="sfaNav">${horizontalNavHtml()}</nav>
          ${crumb}${content}${footer}
        </div>`;
    } else {
      app.innerHTML = `
        <aside class="sfa-sidebar">${brand}<nav class="sfa-nav" id="sfaNav">${verticalNavHtml()}</nav></aside>
        <div class="sfa-main">
          <header class="sfa-topbar">
            <button class="sfa-icon-btn sfa-toggle" id="sfaToggle" title="Toggle sidebar"><i class="bi bi-list"></i></button>
            <div class="sfa-title">${esc(title)}</div>${headerActions(color)}
          </header>
          ${crumb}${content}${footer}
        </div>
        <div class="sfa-backdrop" id="sfaBackdrop"></div>`;
    }

    ensureOffcanvas();
    injectPageContent();
    rewriteLinks();
    wireShell();
    wireHelpTooltip();
    if (window.sfaFlatpickr) sfaFlatpickr(document);
  }

  /** Re-render only the nav + breadcrumb (after a favorite toggle) without touching page content. */
  function refreshNav() {
    const nav = document.getElementById("sfaNav");
    if (nav) nav.innerHTML = navHtml();
    const crumb = document.getElementById("sfaCrumb");
    if (crumb) crumb.innerHTML = crumbBarInner();
    wireNav();
    wireHelpTooltip();
  }

  function injectPageContent() {
    const tpl = document.getElementById("pageContent");
    if (tpl) document.getElementById("sfaContent").appendChild(tpl.content.cloneNode(true));
  }

  function rewriteLinks() {
    document.querySelectorAll('#sfaContent a[href^="/"]').forEach((a) => {
      const h = a.getAttribute("href");
      if (h === "/" || h.startsWith("/?") || /^\/(u\/|api\/|css\/|js\/|favicon)/.test(h)) return;
      a.setAttribute("href", Session.uUrl(u, h));
    });
  }

  function ensureOffcanvas() {
    if (document.getElementById("sfaCustomizer")) return;
    const el = document.createElement("div");
    el.className = "offcanvas offcanvas-end sfa-customizer";
    el.tabIndex = -1;
    el.id = "sfaCustomizer";
    el.innerHTML = `
      <div class="offcanvas-header">
        <h6 class="offcanvas-title"><i class="bi bi-palette"></i> Appearance</h6>
        <button type="button" class="btn-close" data-bs-dismiss="offcanvas"></button>
      </div>
      <div class="offcanvas-body" id="sfaCustomizerBody"></div>`;
    document.body.appendChild(el);
  }

  // The L2 dropdown is absolutely positioned right under its menubar button, so
  // its top is always fine — but its height isn't: the menubar can wrap to two
  // rows on a narrow window, which the static CSS max-height doesn't account
  // for. Clamp it to the real space left below it so it never runs off-screen.
  function clampDropdown(top) {
    const drop = top.querySelector(":scope > .sfa-menu-dropdown");
    if (!drop) return;
    const r = drop.getBoundingClientRect();
    drop.style.maxHeight = Math.max(160, window.innerHeight - r.top - 8) + "px";
  }

  // Flyouts are position:FIXED (see .sfa-menu-flyout in app.css) so they escape
  // the scrolling parent dropdown's overflow clipping — which means their
  // top/left must be computed here rather than inherited from the parent.
  // Anchors to the right of the hovered row, clamps the height to the space
  // actually available, and flips up / to the left near a screen edge.
  function positionFlyout(item) {
    const flyout = item.querySelector(":scope > .sfa-menu-flyout");
    if (!flyout) return;
    const margin = 8;
    const rect = item.getBoundingClientRect();

    // measure natural size with constraints cleared. A still-hidden element
    // measures 0, so force it visible just long enough to read its real size
    // (CSS :hover may not have applied yet when this runs from mouseenter).
    flyout.style.maxHeight = "";
    const hidden = getComputedStyle(flyout).display === "none";
    if (hidden) { flyout.style.visibility = "hidden"; flyout.style.display = "block"; }
    const vh = window.innerHeight, vw = window.innerWidth;
    const needed = flyout.scrollHeight;
    const width = flyout.offsetWidth;
    if (hidden) { flyout.style.display = ""; flyout.style.visibility = ""; }

    // The sticky topbar + menubar own the top of the screen; a flyout must never
    // start above them or its first entries end up hidden behind the header.
    const bar = document.querySelector(".sfa-menubar") || document.querySelector(".sfa-topbar");
    const minTop = Math.max(margin, bar ? bar.getBoundingClientRect().bottom + 4 : margin);
    const maxBottom = vh - margin;

    let maxH = Math.min(needed, maxBottom - minTop);
    let top = rect.top - 6;                       // anchor level with the hovered row
    if (top + maxH > maxBottom) top = maxBottom - maxH;   // pull up so it fits on screen
    if (top < minTop) top = minTop;                       // ...but never under the header
    maxH = Math.min(maxH, maxBottom - top);

    // prefer opening to the right of the row; flip left if it would overflow
    let left = rect.right;
    if (left + width > vw - margin) left = Math.max(margin, rect.left - width);

    flyout.style.top = top + "px";
    flyout.style.left = left + "px";
    flyout.style.maxHeight = Math.max(160, maxH) + "px";
  }

  // ---- event wiring -------------------------------------------------------
  function wireNav() {
    // expand/collapse vertical branches
    document.querySelectorAll("#sfaNav .sfa-nav-toggle").forEach((btn) => {
      btn.addEventListener("click", () => {
        const branch = btn.closest(".sfa-nav-branch");
        const body = branch.querySelector(".sfa-nav-children");
        const open = branch.classList.toggle("open");
        btn.setAttribute("aria-expanded", open);
        if (body) body.hidden = !open;
      });
    });
    // horizontal menu: tap to open the L1 dropdown / L2 flyout (touch devices);
    // CSS :hover still opens them on a desktop with a mouse.
    document.querySelectorAll("#sfaNav .sfa-menu-top-btn").forEach((btn) => {
      btn.addEventListener("click", (e) => {
        e.preventDefault();
        const top = btn.closest(".sfa-menu-top");
        const wasOpen = top.classList.contains("open");
        document.querySelectorAll("#sfaNav .sfa-menu-top.open, #sfaNav .sfa-menu-item.open")
          .forEach((el) => el.classList.remove("open"));
        if (!wasOpen) { top.classList.add("open"); clampDropdown(top); }
      });
    });
    document.querySelectorAll("#sfaNav .sfa-menu-top").forEach((top) => {
      top.addEventListener("mouseenter", () => clampDropdown(top));
    });
    document.querySelectorAll("#sfaNav .sfa-menu-sub-toggle").forEach((btn) => {
      btn.addEventListener("click", (e) => {
        e.preventDefault(); e.stopPropagation();
        const item = btn.closest(".sfa-menu-item");
        const open = item.classList.toggle("open");
        if (open) positionFlyout(item);
      });
    });
    // CSS :hover also reveals a flyout (desktop mouse) without going through
    // the click handler above — reposition on hover too, or a long list (e.g.
    // Mapping) opened by mouse would still run off the bottom of the screen.
    document.querySelectorAll("#sfaNav .sfa-menu-item.has-sub").forEach((item) => {
      item.addEventListener("mouseenter", () => positionFlyout(item));
    });
    bindMenuOutsideClose();
    // favorite star toggles
    document.querySelectorAll(".sfa-fav-btn").forEach((btn) => {
      btn.addEventListener("click", async (e) => {
        e.preventDefault(); e.stopPropagation();
        const id = Number(btn.getAttribute("data-fav"));
        const on = FAV.has(id);
        try {
          const updated = on ? await Api.del(API.menu.favorite(id)) : await Api.post(API.menu.favorite(id));
          FAV = new Set(updated || []);
          // keep the cached menu response's favourites in sync so the NEXT page
          // navigation's cache-hit still reflects this change (no menu refetch).
          const cached = Session.cache.get("menu");
          if (cached) { cached.favorites = updated || []; Session.cache.set("menu", cached); }
          refreshNav();
        } catch (err) { SFA.toast(err.message, false); }
      });
    });
  }

  function closeDrawer() { document.getElementById("app").classList.remove("sidebar-open"); }

  // Rebuild the chrome when the viewport crosses the mobile breakpoint and the
  // effective layout flips (e.g. rotating a tablet, resizing a desktop window).
  let _resizeBound = false;
  function bindResize() {
    if (_resizeBound) return;
    _resizeBound = true;
    let t = null;
    window.addEventListener("resize", () => {
      clearTimeout(t);
      t = setTimeout(() => {
        const app = document.getElementById("app");
        if (app && currentLayout() !== app.getAttribute("data-layout")) Shell.rebuild();
      }, 180);
    });
  }

  // Live footer clock — ticks #sfaClockNow every second. Re-queries the
  // element each tick (rather than caching it) so it keeps working across
  // Shell.rebuild()'s full innerHTML replacement without re-binding.
  let _clockBound = false;
  function bindClock() {
    if (_clockBound) return;
    _clockBound = true;
    setInterval(() => {
      const el = document.getElementById("sfaClockNow");
      if (el) el.textContent = fmtDateTime(new Date());
    }, 1000);
  }

  /**
   * Footer online/offline indicator (#sfaNetStatus) — like WhatsApp Web's
   * connection banner. THREE signals combined, so a server restart (e.g. a
   * local Tomcat/devtools restart) is caught almost immediately instead of
   * possibly being missed between polls:
   *   - the browser's own online/offline EVENTS fire instantly (no polling lag)
   *   - EVERY real Api.* call (js/core/http.js) reports success/failure here
   *     directly — normal app usage alone (lookups, list fetches, etc.) is
   *     usually enough to notice a restart within one click, no polling needed
   *   - a periodic real fetch to /api/public/ping as a backup for when the
   *     user is idle and no other request is happening. Needed at all because
   *     navigator.onLine only reflects "does this device have a network
   *     interface up" — it stays TRUE on a wifi with no real internet, or if
   *     OUR server specifically (not the internet at large) is down.
   */
  let netOnline = true;
  let _netBound = false;

  function setNetStatus(state) {   // 'online' | 'offline' | 'checking'
    const el = document.getElementById("sfaNetStatus");
    if (!el) return;
    el.className = "sfa-net-status sfa-net-" + state;
    const t = el.querySelector(".sfa-net-text");
    if (!t) return;
    const tr = (k, d) => (window.I18n ? I18n.t(k, d) : d);
    t.textContent = state === "online" ? tr("footer.online", "Online")
      : state === "offline" ? tr("footer.offline", "Offline") : tr("footer.reconnecting", "Reconnecting…");
  }

  function markOnline(announce) {
    const changed = !netOnline;
    netOnline = true;
    setNetStatus("online");
    if (announce && changed && window.SFA) SFA.toast("Back online", true);
  }

  function markOffline(announce) {
    const changed = netOnline;
    netOnline = false;
    setNetStatus("offline");
    if (announce && changed && window.SFA) SFA.toast("You're offline — check your internet connection", false);
  }

  // Called directly from js/core/http.js on every single API response/failure
  // — the fast path. Silent (no duplicate toast) when nothing actually changed.
  window.sfaReportNetworkOk = () => markOnline(true);
  window.sfaReportNetworkError = () => markOffline(true);

  async function pingReachable() {
    try {
      const ctrl = new AbortController();
      const timer = setTimeout(() => ctrl.abort(), 4000);
      // prefix with the deploy context (e.g. /sfaexpert) so the ping routes
      // through the reverse proxy — a raw /api/... would 404 and falsely read
      // as "offline" right after the page's prefixed API calls said "online".
      const res = await fetch((window.Session ? Session.ctx() : "") + API.ping, { cache: "no-store", signal: ctrl.signal });
      clearTimeout(timer);
      // ANY HTTP response — even a 404 / 3xx from a reverse proxy — proves the
      // SERVER is reachable, which is all this footer indicator means. This is the
      // exact rule the real Api hook already uses (see sfaReportNetworkOk in
      // js/core/http.js). Returning res.ok instead made a proxied deploy flap to
      // "offline" whenever /api/public/ping happened to answer non-2xx behind the
      // proxy, even while every real API call was succeeding. Only a thrown fetch
      // (connection refused / DNS / abort) — caught below — is a real "offline".
      return true;
    } catch (e) { return false; }
  }

  async function refreshNetStatus(announce) {
    const reachable = await pingReachable();
    if (reachable) markOnline(announce); else markOffline(announce);
  }

  function bindNetworkStatus() {
    setNetStatus(navigator.onLine ? "checking" : "offline");
    refreshNetStatus(false);   // initial check, silent (no toast on first paint)
    if (_netBound) return;
    _netBound = true;
    window.addEventListener("online", () => { setNetStatus("checking"); refreshNetStatus(true); });
    window.addEventListener("offline", () => markOffline(true));
    // Idle-only backup — most restarts are caught by the http.js hook above
    // the moment the user's next click fires a real request. 5s (not 15s) so
    // even a quiet tab notices a local restart quickly.
    setInterval(() => refreshNetStatus(true), 5000);
  }

  // Close any open horizontal dropdown when tapping/clicking outside it.
  let _menuCloseBound = false;
  function bindMenuOutsideClose() {
    if (_menuCloseBound) return;
    _menuCloseBound = true;
    document.addEventListener("click", (e) => {
      if (!e.target.closest(".sfa-menu-top")) {
        document.querySelectorAll("#sfaNav .sfa-menu-top.open, #sfaNav .sfa-menu-item.open")
          .forEach((el) => el.classList.remove("open"));
      }
    });
  }

  function wireShell() {
    const toggle = document.getElementById("sfaToggle");
    if (toggle) toggle.addEventListener("click", () => {
      const app = document.getElementById("app");
      if (isNarrow()) app.classList.toggle("sidebar-open");           // slide-in drawer
      else {
        app.classList.toggle("sidebar-collapsed");                    // icon rail
        if (window.Theme && Theme.state) Theme.save({ sidebarCollapsed: app.classList.contains("sidebar-collapsed") }).catch(() => {});
      }
    });
    // tap the dimmed backdrop to close the mobile drawer
    const backdrop = document.getElementById("sfaBackdrop");
    if (backdrop) backdrop.addEventListener("click", closeDrawer);
    // remember the icon-rail choice only on desktop; mobile always uses the drawer
    if (!isNarrow() && window.Theme && Theme.state && Theme.state.sidebarCollapsed && savedLayout() !== "horizontal") {
      document.getElementById("app").classList.add("sidebar-collapsed");
    }
    bindResize();

    // Revoke the refresh token server-side on sign-out (best-effort) so it can't
    // be used to silently mint new access tokens after the user has logged out.
    const so = document.getElementById("signOutThis");
    const soa = document.getElementById("signOutAll");
    if (so) so.addEventListener("click", async (e) => {
      e.preventDefault();
      const acc = Session.list()[u];
      if (acc && acc.refreshToken) {
        try { await Api.post(API.auth.logout, { refreshToken: acc.refreshToken }, { noAuthRedirect: true }); } catch (err) { /* best-effort */ }
      }
      Session.signOut(u);
    });
    if (soa) soa.addEventListener("click", async (e) => {
      e.preventDefault();
      const accounts = Session.list();
      await Promise.all(accounts.map((a) => a.refreshToken
        ? Api.post(API.auth.logout, { refreshToken: a.refreshToken }, { noAuthRedirect: true }).catch(() => {})
        : Promise.resolve()));
      Session.signOutAll();
    });

    const modeBtn = document.getElementById("sfaModeToggle");
    if (modeBtn) modeBtn.addEventListener("click", () => window.Theme && Theme.toggleMode().catch((e) => SFA.toast(e.message, false)));

    const czBtn = document.getElementById("sfaCustomize");
    if (czBtn) czBtn.addEventListener("click", () => {
      if (!window.Theme || !Theme.state) return;
      Theme.buildCustomizer(document.getElementById("sfaCustomizerBody"));
      bootstrap.Offcanvas.getOrCreateInstance(document.getElementById("sfaCustomizer")).show();
    });

    wireLanguage();
    wireNav();
    wireSearch();
  }

  /** Populate the topbar language dropdown from I18n.available and switch on click. */
  function wireLanguage() {
    const menu = document.getElementById("sfaLangMenu");
    if (!menu || !window.I18n) return;
    const cur = I18n.lang;
    const list = I18n.available;
    // show the ACTIVE language's flag on the topbar button
    const active = list.find((l) => l.code === cur);
    const flagEl = document.getElementById("sfaLangFlag");
    if (flagEl && active) flagEl.textContent = active.flag || "🌐";
    menu.innerHTML = list.map((l) =>
      `<li><a class="dropdown-item d-flex align-items-center justify-content-between gap-3 ${l.code === cur ? "active" : ""}"
             href="#" data-lang="${l.code}">
             <span><span class="sfa-lang-flag">${l.flag || "🌐"}</span> ${esc(l.label)}</span>
             ${l.code === cur ? '<i class="bi bi-check2"></i>' : ""}</a></li>`).join("");
    menu.querySelectorAll("a[data-lang]").forEach((a) => {
      a.addEventListener("click", async (e) => {
        e.preventDefault();
        const code = a.getAttribute("data-lang");
        if (code === I18n.lang) return;
        try {
          // persist per-user + refresh the cached bundle (the ONE server call),
          // then reload: the page re-renders cleanly in the new language straight
          // from cache (no second server hit), avoiding stale DataTables header
          // clones and any half-translated dynamic DOM.
          await I18n.setLang(code);
          location.reload();
        } catch (err) { if (window.SFA) SFA.toast(err.message, false); }
      });
    });
  }

  function wireSearch() {
    const input = document.getElementById("sfaSearch");
    const box = document.getElementById("sfaSearchResults");
    if (!input || !box) return;
    const all = leaves(MENU, [], []);
    const render = (q) => {
      const term = q.trim().toLowerCase();
      const hits = !term ? [] : all.filter((l) =>
        l.label.toLowerCase().includes(term) || l.path.toLowerCase().includes(term)).slice(0, 8);
      if (!hits.length) { box.hidden = true; box.innerHTML = ""; return; }
      box.innerHTML = hits.map((h) =>
        `<a href="${Session.uUrl(u, h.href)}"><i class="bi bi-${h.icon || "dot"}"></i>
          <span><b>${esc(h.label)}</b><small>${esc(h.path)}</small></span></a>`).join("");
      box.hidden = false;
    };
    input.addEventListener("input", () => render(input.value));
    input.addEventListener("focus", () => render(input.value));
    input.addEventListener("keydown", (e) => {
      if (e.key === "Enter") { const first = box.querySelector("a"); if (first) location.href = first.getAttribute("href"); }
      else if (e.key === "Escape") { box.hidden = true; }
    });
    document.addEventListener("click", (e) => { if (!e.target.closest(".sfa-search")) box.hidden = true; });
  }

  // ---- SFA page-facing helpers (backed by the core modules) --------------
  window.SFA = {
    get token() { return Session.token(); },
    get claims() { return Session.claims(); },
    /** SFA.can("ZONE_SAVE") — false only when this user is explicitly denied. */
    can,
    /** Drop the cached permission map (e.g. after editing assignments) and re-apply. */
    async reloadPermissions() { Session.cache.clear("perms"); await loadPermissions(); applyPermissions(document); },
    user, companyCode, role, u, esc,
    me() { return Api.get(API.me); },
    money: (n) => (n == null || n === "" ? "" : "₹ " + Number(n).toLocaleString("en-IN", { minimumFractionDigits: 2 })),
    statusBadge: (st) => st === "Y"
      ? '<span class="badge badge-active">Active</span>'
      : '<span class="badge badge-inactive">Inactive</span>',
    fillSelect(sel, items, placeholder) {
      sel.innerHTML = '<option value="">' + (placeholder || "--") + "</option>" +
        (items || []).map((i) => `<option value="${i.oid}">${esc(i.label)}</option>`).join("");
    },
    api(path, opts) {
      opts = opts || {};
      const method = (opts.method || "GET").toUpperCase();
      const cfg = { headers: opts.headers };
      if (method === "GET") return Api.get(path, cfg);
      if (method === "DELETE") return Api.del(path, cfg);
      if (method === "PUT") return Api.put(path, opts.body, cfg);
      if (method === "PATCH") return Api.patch(path, opts.body, cfg);
      return Api.post(path, opts.body, cfg);
    },
    download(path, filename) { return Api.download(path, filename); },
    toast(msg, ok) {
      const el = document.getElementById("sfaMsg");
      if (!el) return;
      el.className = "alert py-2 small " + (ok === false ? "alert-danger" : "alert-success");
      el.textContent = msg;
      el.style.display = "block";
      setTimeout(() => { el.style.display = "none"; }, ok === false ? 8000 : 3000);
    },
  };

  window.Shell = {
    rebuild() { build(); if (window.pageInit) { try { window.pageInit(); } catch (e) { /* re-init */ } } if (window.UIKit) UIKit.init(); },
    refreshNav,
    // re-fetch the logo + user photo and re-render the brand/avatar in place
    async refreshIdentity() { await loadIdentity(true); applyIdentity(); },
  };

  // ---- boot ---------------------------------------------------------------
  document.addEventListener("DOMContentLoaded", async function () {
    if (window.Theme) Theme.applyCached();
    if (window.I18n) I18n.applyCached();
    // fetch theme + menu before building so layout and nav are correct first paint
    await Promise.all([
      (window.Theme && !Theme.state) ? Theme.load().catch(() => {}) : Promise.resolve(),
      window.I18n ? I18n.load().catch(() => {}) : Promise.resolve(),
      loadMenu(),
      loadIdentity(),
      loadPermissions(),
      // saved column layouts for any data-screen-key grid on this page (app.js)
      window.sfaColumnPrefsPreload ? sfaColumnPrefsPreload().catch(() => {}) : Promise.resolve(),
      window.Settings ? Settings.load().catch(() => {}) : Promise.resolve(),
      // Deliberately its OWN live server call, not from the Settings cache above —
      // see the big comment on sfaApplyContentProtection in app.js for why.
      window.sfaApplyContentProtection ? sfaApplyContentProtection().catch(() => {}) : Promise.resolve(),
    ]);
    build();
    // build() just created the chrome (topbar/footer/menu) and injected the
    // page template — translate all of it now that it's in the DOM.
    if (window.I18n) I18n.apply();
    bindClock();
    bindNetworkStatus();
    watchPermissions();
    if (window.pageInit) { try { window.pageInit(); } catch (e) { console.error(e); } }
    // pageInit may have injected more DOM (tables, modals) — re-translate.
    if (window.I18n) I18n.apply();
    // global keyboard + UX layer (filter collapse, key shortcuts, mobile cards)
    if (window.UIKit) UIKit.init();
    // background refresh (covers first-ever load where nothing was cached).
    // Theme is already fresh from the Promise.all above (fetched-or-cached) —
    // no second /api/theme call needed. Just check the EFFECTIVE layout
    // (mobile-aware) against what build() actually rendered — not the saved
    // navLayout — otherwise a "horizontal" preference on a phone would keep
    // triggering rebuilds against the forced "vertical" drawer.
    const app = document.getElementById("app");
    if (app && currentLayout() !== app.getAttribute("data-layout")) Shell.rebuild();
  });
})(window);
