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
  if (!account || !account.token) { location.replace("/"); return; }

  const claims = Session.claims();
  const user = account.user || claims.sub || "user";
  const tenant = account.tenant || claims.tenant || "?";
  const role = claims.role || "";
  const u = Session.activeIndex();

  const esc = (s) => (s == null ? "" : String(s)
    .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;"));

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

  async function loadMenu() {
    try {
      const res = await Api.get(API.menu.get);
      MENU = (res && res.items) || [];
      FAV = new Set((res && res.favorites) || []);
      try { localStorage.setItem("sfa_menu", JSON.stringify(res)); } catch (e) {}
      if (!MENU.length) MENU = DEFAULT_MENU;
    } catch (e) {
      // offline / not migrated yet: try cache, then the built-in default
      try {
        const c = JSON.parse(localStorage.getItem("sfa_menu"));
        MENU = (c && c.items && c.items.length) ? c.items : DEFAULT_MENU;
        FAV = new Set((c && c.favorites) || []);
      } catch (e2) { MENU = DEFAULT_MENU; FAV = new Set(); }
    }
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
      if (hasHref(n)) out.push({ id: n.id, label: n.label, path: t.join(" › "), href: n.href, icon: n.icon, logo: n.logo });
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

  /** Breadcrumb bar inner: trail on the left, the page/report title on the right (same line). */
  function crumbBarInner() {
    return `<div class="sfa-crumb-trail">${breadcrumbHtml()}</div>` +
           `<div class="sfa-crumb-title">${esc(pageTitle())}</div>`;
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
        <a href="${Session.uUrl(u, n.href)}" class="sfa-nav-link level-${level} ${active ? "active" : ""}">
          ${iconMarkup(n)}<span class="sfa-nav-text">${esc(n.label)}</span></a>${starMarkup(n)}
      </div>`;
  }

  function verticalNavHtml() {
    const favs = favoriteLeaves();
    let html = "";
    if (favs.length) {
      html += `<div class="sfa-nav-branch level-1 open sfa-fav-group">
          <div class="sfa-nav-fav-head"><i class="bi bi-star-fill"></i><span class="sfa-nav-text">Favorites</span></div>
          <div class="sfa-nav-children">${favs.map((f) => `
            <div class="sfa-nav-leaf-row">
              <a href="${Session.uUrl(u, f.href)}" class="sfa-nav-link level-2 ${f.page === activePage() ? "active" : ""}">
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
        <a href="${Session.uUrl(u, n.href)}" class="sfa-menu-leaf ${active ? "active" : ""}">${iconMarkup(n)}<span>${esc(n.label)}</span></a>${starMarkup(n)}
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
    return `<a href="${Session.uUrl(u, n.href)}" class="sfa-menu-top sfa-menu-top-link ${active ? "active" : ""}">${iconMarkup(n)}<span>${esc(n.label)}</span></a>`;
  }

  function horizontalNavHtml() {
    const favs = favoriteLeaves();
    let html = "";
    if (favs.length) {
      html += `<div class="sfa-menu-top">
          <button type="button" class="sfa-menu-top-btn"><i class="bi bi-star-fill"></i><span>Favorites</span><i class="bi bi-chevron-down sfa-caret"></i></button>
          <div class="sfa-menu-dropdown">${favs.map((f) => `
            <div class="sfa-menu-leaf-row">
              <a href="${Session.uUrl(u, f.href)}" class="sfa-menu-leaf ${f.page === activePage() ? "active" : ""}">${iconMarkup(f)}<span>${esc(f.label)}</span></a>
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

  async function loadIdentity() {
    try {
      const [b, p] = await Promise.all([
        Api.get(API.branding.get).catch(() => ({})),
        Api.get(API.profile.get).catch(() => ({})),
      ]);
      IDENTITY = { logo: b.logo || null, companyName: b.companyName || null, photo: (p && p.photo) || null };
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
            <small class="text-muted">tenant: ${esc(a.tenant)} &middot; /u/${i}</small></span></a>`;
    }).join("");
    return `${items}
      <div class="dropdown-divider"></div>
      <a class="dropdown-item" href="/?add=1"><i class="bi bi-person-plus"></i> Add another account</a>
      <a class="dropdown-item" href="${Session.uUrl(u, "settings/profile.html")}"><i class="bi bi-person-circle"></i> My Profile</a>
      <a class="dropdown-item" href="${Session.uUrl(u, "settings/appearance.html")}"><i class="bi bi-palette"></i> Appearance</a>
      <a class="dropdown-item" href="#" id="signOutThis"><i class="bi bi-box-arrow-right"></i> Sign out <b>${esc(user)}</b></a>
      <a class="dropdown-item text-danger" href="#" id="signOutAll"><i class="bi bi-power"></i> Sign out all accounts</a>`;
  }

  function headerActions(color) {
    return `<div class="sfa-actions">
        <div class="sfa-search">
          <i class="bi bi-search"></i>
          <input type="text" id="sfaSearch" placeholder="Search menu…" autocomplete="off" />
          <div class="sfa-search-results" id="sfaSearchResults" hidden></div>
        </div>
        <button class="sfa-icon-btn" id="sfaModeToggle" title="Toggle light / dark"><i class="bi bi-circle-half"></i></button>
        <button class="sfa-icon-btn" id="sfaCustomize" title="Customize appearance"><i class="bi bi-palette"></i></button>
        <div class="sfa-user dropdown">
          <a href="#" class="dropdown-toggle d-flex align-items-center gap-2" data-bs-toggle="dropdown">
            <span id="sfaHeaderAvatar">${avatarMarkup(color, user, IDENTITY.photo)}</span>
            <span class="d-none d-md-inline">${esc(user)}
              ${role ? `<span class="badge bg-secondary" style="font-size:10px">${esc(role)}</span>` : ""}
              <small class="text-muted">(${esc(tenant)} &middot; /u/${u})</small></span>
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
    return (window.Theme && Theme.state && Theme.state.navLayout) ||
      localStorage.getItem("sfa_nav") || "vertical";
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
    const footer = `<footer class="sfa-footer">
        <span>&copy; ${new Date().getFullYear()} StarSFA · Sales Force Automation</span>
        <span class="sfa-footer-right">tenant <b>${esc(tenant)}</b> · signed in as <b>${esc(user)}</b></span>
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
  }

  /** Re-render only the nav + breadcrumb (after a favorite toggle) without touching page content. */
  function refreshNav() {
    const nav = document.getElementById("sfaNav");
    if (nav) nav.innerHTML = navHtml();
    const crumb = document.getElementById("sfaCrumb");
    if (crumb) crumb.innerHTML = crumbBarInner();
    wireNav();
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
        if (!wasOpen) top.classList.add("open");
      });
    });
    document.querySelectorAll("#sfaNav .sfa-menu-sub-toggle").forEach((btn) => {
      btn.addEventListener("click", (e) => {
        e.preventDefault(); e.stopPropagation();
        btn.closest(".sfa-menu-item").classList.toggle("open");
      });
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

    const so = document.getElementById("signOutThis");
    const soa = document.getElementById("signOutAll");
    if (so) so.addEventListener("click", (e) => { e.preventDefault(); Session.signOut(u); });
    if (soa) soa.addEventListener("click", (e) => { e.preventDefault(); Session.signOutAll(); });

    const modeBtn = document.getElementById("sfaModeToggle");
    if (modeBtn) modeBtn.addEventListener("click", () => window.Theme && Theme.toggleMode().catch((e) => SFA.toast(e.message, false)));

    const czBtn = document.getElementById("sfaCustomize");
    if (czBtn) czBtn.addEventListener("click", () => {
      if (!window.Theme || !Theme.state) return;
      Theme.buildCustomizer(document.getElementById("sfaCustomizerBody"));
      bootstrap.Offcanvas.getOrCreateInstance(document.getElementById("sfaCustomizer")).show();
    });

    wireNav();
    wireSearch();
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
    user, tenant, role, u, esc,
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
    rebuild() { build(); if (window.pageInit) { try { window.pageInit(); } catch (e) { /* re-init */ } } },
    refreshNav,
    // re-fetch the logo + user photo and re-render the brand/avatar in place
    async refreshIdentity() { await loadIdentity(); applyIdentity(); },
  };

  // ---- boot ---------------------------------------------------------------
  document.addEventListener("DOMContentLoaded", async function () {
    if (window.Theme) Theme.applyCached();
    // fetch theme + menu before building so layout and nav are correct first paint
    await Promise.all([
      (window.Theme && !Theme.state) ? Theme.load().catch(() => {}) : Promise.resolve(),
      loadMenu(),
      loadIdentity(),
    ]);
    build();
    if (window.pageInit) { try { window.pageInit(); } catch (e) { console.error(e); } }
    // background refresh (covers first-ever load where nothing was cached).
    // Compare against the EFFECTIVE layout (mobile-aware) — not the saved
    // navLayout — otherwise a "horizontal" preference on a phone would keep
    // triggering rebuilds against the forced "vertical" drawer.
    if (window.Theme) Theme.load().then(() => {
      const app = document.getElementById("app");
      if (app && currentLayout() !== app.getAttribute("data-layout")) Shell.rebuild();
    }).catch(() => {});
  });
})(window);
