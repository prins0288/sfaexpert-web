/* ============================================================================
 * I18n — the language / labels engine.
 *
 * The server resolves (english base -> chosen language -> tenant label_master
 * override) into ONE flat { key: value } map and hands it back with a content
 * `version`. This module downloads that bundle ONCE, caches it in localStorage
 * (instant paint on the next load) AND in Session.cache (sessionStorage, so
 * repeat navigations in a tab make NO network call at all), and applies it to
 * the DOM.
 *
 * Applying:
 *   [data-i18n="key"]        -> element.textContent
 *   [data-i18n-ph="key"]     -> element.placeholder
 *   [data-i18n-title="key"]  -> element.title (tooltip)
 * A key that isn't in the bundle is left ALONE — the element keeps its original
 * (English) markup, so a missing translation never shows a "?" or a raw key.
 *
 * The chosen language is PER USER (saved server-side). The cache is scoped to
 * the account slot in the URL (/u/0, /u/1...) because tenant overrides make the
 * resolved bundle tenant-specific.
 * ==========================================================================*/
(function (window) {
  "use strict";

  function acctSuffix() {
    const m = location.pathname.match(/^\/u\/(\d+)(\/|$)/);
    return "_u" + (m ? m[1] : "0");
  }
  const CACHE = "sfa_i18n" + acctSuffix();     // { lang, version, available, labels }

  let state = null;                             // current bundle
  const listeners = [];

  function readCache() {
    try { return JSON.parse(localStorage.getItem(CACHE)); } catch (e) { return null; }
  }
  function writeCache(b) {
    try { localStorage.setItem(CACHE, JSON.stringify(b)); } catch (e) { /* non-fatal */ }
    try { if (window.Session) Session.cache.set("i18n", b); } catch (e) {}
    // <html lang> helps the browser (spell-check, hyphenation) and any CSS.
    try { document.documentElement.setAttribute("lang", b.lang || "en"); } catch (e) {}
  }

  /** Look up one label; returns the key itself only if truly unknown (defensive —
   *  the server's english base guarantees every shipped key resolves). */
  function t(key, fallback) {
    if (state && state.labels && Object.prototype.hasOwnProperty.call(state.labels, key)) {
      return state.labels[key];
    }
    return fallback != null ? fallback : key;
  }

  /**
   * Auto-translate dictionary: English chrome phrase -> label key. Used to
   * translate the shared UI (toolbar buttons, table headers, form labels,
   * modal titles, DataTables buttons) WITHOUT hand-tagging every element on
   * every page. It is applied ONLY within "safe" selectors (never <tbody>,
   * never <option>), so real data rows and lookup values are never touched.
   * Matching is on the element's original English text; the resolved key is
   * then stamped as data-i18n so it's idempotent and language-switch works.
   */
  const PHRASE = {
    "Add": "common.add", "Add Single": "common.addSingle", "Add Multiple": "common.addMultiple",
    "Edit": "common.edit", "Delete": "common.delete", "Save": "common.save", "Cancel": "common.cancel",
    "Close": "common.close", "Reset": "common.reset", "Search": "common.search", "Search:": "common.searchColon",
    "Actions": "common.actions", "Status": "common.status", "Active": "common.active", "Inactive": "common.inactive",
    "Code": "common.code", "Name": "common.name", "Upload": "common.upload", "Bulk Upload": "common.bulkUpload",
    "Download": "common.download", "Download Template": "common.downloadTemplate", "Filter": "common.filter",
    "Columns": "common.columns", "Excel": "common.excel", "All": "common.all", "S.No": "common.sno",
    "Add Row": "common.addRow", "Description": "common.description", "Icon": "common.icon",
    "Remarks": "common.remarks", "City": "common.city", "Pincode": "common.pincode", "Mobile": "common.mobile",
    "Email": "common.email", "Address": "common.address", "Type": "common.type", "Date": "common.date",
    "From": "common.from", "To": "common.to",
    "Division": "entity.division", "Divisions": "entity.divisions", "Zone": "entity.zone", "Zones": "entity.zones",
    "State": "entity.state", "States": "entity.states", "HQ": "entity.hq", "HQs": "entity.hqs",
    "Country": "entity.country", "Countries": "entity.countries", "Area": "entity.area", "Areas": "entity.areas",
    "Route": "entity.route", "Routes": "entity.routes", "Client": "entity.client", "Clients": "entity.clients",
    "Client Type": "entity.clientType", "Client Types": "entity.clientTypes",
    "Category": "entity.category", "Categories": "entity.categories",
    "Speciality": "entity.speciality", "Specialities": "entity.specialities",
    "Degree": "entity.degree", "Designation": "entity.designation", "District": "entity.district",
    "Employee": "entity.employee", "Employees": "entity.employees", "Product": "entity.product", "Products": "entity.products"
  };
  // Where auto-translate is allowed to look. Deliberately excludes tbody/option
  // (real data + lookup values) — only chrome: headers, toolbar/modal buttons,
  // form labels, legends, DataTables buttons.
  const AUTO_SEL = "thead th, .card-header, .card-tools .btn, .modal-title, .form-label, .sfa-legend, " +
    ".dt-button, .dt-buttons .btn, .modal-content .btn, .multi-row-table thead th";

  /** Trim, collapse spaces, drop a trailing required-star "*". */
  function sig(text) { return (text || "").replace(/\s+/g, " ").replace(/\s*\*\s*$/, "").trim(); }

  /**
   * Set an element's LABEL text without clobbering a child icon (<i>), a
   * trailing required-star span, or a text-bearing wrapper (DataTables wraps a
   * button's caption in its own <span>). Finds the FIRST meaningful text node
   * anywhere in the subtree (document order) and replaces it in place; only
   * appends if the element truly has no text yet.
   */
  function firstTextNode(el) {
    for (const n of el.childNodes) {
      if (n.nodeType === 3) { if (n.nodeValue.trim()) return n; }
      else if (n.nodeType === 1) { const d = firstTextNode(n); if (d) return d; }
    }
    return null;
  }
  function setLabelText(el, text) {
    const n = firstTextNode(el);
    if (n) {
      const lead = /^\s/.test(n.nodeValue) ? " " : "";
      const trail = /\s$/.test(n.nodeValue) ? " " : "";
      n.nodeValue = lead + text + trail;
    } else {
      el.appendChild(document.createTextNode(el.querySelector("*") ? " " + text : text));
    }
  }

  /** Translate every tagged element inside `scope` (default: whole document). */
  function apply(scope) {
    if (!state || !state.labels) return;
    const root = scope || document;
    const L = state.labels;
    const has = (k) => Object.prototype.hasOwnProperty.call(L, k);

    // 1) explicit data-i18n tags (icon/star-safe)
    root.querySelectorAll("[data-i18n]").forEach((el) => {
      const k = el.getAttribute("data-i18n");
      if (has(k)) setLabelText(el, L[k]);
    });
    root.querySelectorAll("[data-i18n-ph]").forEach((el) => {
      const k = el.getAttribute("data-i18n-ph");
      if (has(k)) el.setAttribute("placeholder", L[k]);
    });
    root.querySelectorAll("[data-i18n-title]").forEach((el) => {
      const k = el.getAttribute("data-i18n-title");
      if (has(k)) el.setAttribute("title", L[k]);
    });

    // 2) dictionary auto-translate within safe selectors only (never tbody/option)
    root.querySelectorAll(AUTO_SEL).forEach((el) => {
      if (el.hasAttribute("data-i18n") || el.getAttribute("data-i18n-skip") === "1") return;
      const key = PHRASE[sig(el.textContent)];
      if (!key || !has(key)) return;
      setLabelText(el, L[key]);
      el.setAttribute("data-i18n", key);   // stamp -> idempotent + follows future switches
    });

    listeners.forEach((fn) => { try { fn(state); } catch (e) {} });
  }

  window.I18n = {
    get state() { return state; },
    get lang() { return state ? state.lang : "en"; },
    get available() { return (state && state.available) || [{ code: "en", label: "English", flag: "🇬🇧" }]; },
    t: t,
    onChange(fn) { if (typeof fn === "function") listeners.push(fn); },

    /** Translate tagged elements in `scope` (default document). Safe to call
     *  repeatedly as new DOM is injected; a page can call I18n.apply(node). */
    apply(scope) { apply(scope); },

    /** Paint from the last-known bundle immediately (no network). */
    applyCached() {
      const b = readCache();
      if (b) { state = b; apply(); }
      return b;
    },

    /**
     * Ensure a bundle is loaded and applied. Uses the per-tab Session.cache
     * first (no network), else the server (once), matching Theme/menu/settings.
     */
    async load() {
      const cached = window.Session && Session.cache.get("i18n");
      if (cached) { state = cached; writeCache(cached); apply(); return cached; }
      if (!window.API || !window.Api) return state;
      const b = await Api.get(API.i18n.bundle());   // server picks the user's saved language
      state = b; writeCache(b); apply();
      return b;
    },

    /** Switch language: persist per-user, refresh caches, re-apply everywhere. */
    async setLang(code) {
      const b = await Api.put(API.i18n.setLang(code));
      state = b; writeCache(b); apply();
      return b;
    },
  };
})(window);
