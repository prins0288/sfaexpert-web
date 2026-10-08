/* ============================================================================
 * Session — the Gmail-style multi-account JWT store (/u/N URLs).
 *
 * One JWT per signed-in account kept in localStorage; the /u/{index}/ URL
 * segment selects which account is active. This is the SINGLE definition of the
 * account store — the login page and the app shell both use it (previously the
 * logic was copy-pasted in two places).
 *
 * Each account also carries a long-lived refreshToken (opaque, from
 * LoginResponse) — see js/core/http.js, which silently exchanges it for a new
 * access token on a 401 instead of forcing the user to log in again.
 * ==========================================================================*/
(function (window) {
  "use strict";

  /**
   * Bump this after any change that alters what's INSIDE a cached response's
   * shape (a new theme token, a new menu item, a new setting key, etc.) —
   * every "common data" cache this app keeps client-side (this file's
   * per-tab sessionStorage `cache`, plus theme.js's/shell.js's/settings.js's
   * own localStorage "paint instantly before the network reply" caches) gets
   * wiped automatically on the next page load, in every open tab/browser, no
   * manual "clear site data" needed. Without this, a browser that already
   * cached the OLD shape would keep serving it indefinitely (sessionStorage
   * survives regular reloads; localStorage survives even closing the tab).
   */
  const CACHE_VERSION = "7";
  const VERSION_KEY = "sfa_cache_version";
  (function bustStaleCaches() {
    try {
      if (localStorage.getItem(VERSION_KEY) === CACHE_VERSION) return;
      // v3: the theme became PER TENANT and its localStorage keys gained a
      // "_u<account>" suffix — the old unsuffixed keys are shared across every
      // signed-in tenant, so they must be dropped or one tenant's colours keep
      // painting on another. Also clears any suffixed entry, so a version bump
      // still forces a clean refetch.
      ["sfa_theme", "sfa_menu", "sfa_identity", "sfa_settings",
       "sfa_mode", "sfa_nav", "sfa_density", "sfa_font", "sfa_i18n", "sfa_loader"].forEach((k) => localStorage.removeItem(k));
      Object.keys(localStorage)
        .filter((k) => /^sfa_(theme|menu|identity|settings|mode|nav|density|font|i18n|loader)_u\d+$/.test(k))
        .forEach((k) => localStorage.removeItem(k));
      Object.keys(sessionStorage)
        .filter((k) => k.indexOf("sfa_cache_") === 0)
        .forEach((k) => sessionStorage.removeItem(k));
      localStorage.setItem(VERSION_KEY, CACHE_VERSION);
    } catch (e) { /* storage disabled — nothing to bust */ }
  })();

  const KEY = "sfa_accounts";

  function list() {
    try { return JSON.parse(localStorage.getItem(KEY)) || []; }
    catch (e) { return []; }
  }
  function saveList(arr) { localStorage.setItem(KEY, JSON.stringify(arr)); }

  function decode(token) {
    try {
      const p = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
      return JSON.parse(decodeURIComponent(escape(atob(p))));
    } catch (e) { return {}; }
  }

  /**
   * Deployment context prefix — "" when served at the site root (local dev,
   * localhost:8081) or "/sfaexpert" when behind a reverse proxy that mounts the
   * app under a sub-path. Derived from the URL: the segment before "/u/<n>/",
   * or the current directory on the root/login page. Every JS-built URL (uUrl,
   * the API base in http.js) prepends this, and each page's inline boot script
   * writes a matching <base> so relative CSS/JS also resolve under the prefix.
   */
  function ctx() {
    const p = location.pathname;
    const m = p.match(/^(.*?)\/u\/\d+(\/|$)/);
    return m ? m[1] : p.replace(/\/[^\/]*$/, "");
  }

  function activeIndex() {
    const m = location.pathname.slice(ctx().length).match(/^\/u\/(\d+)(\/|$)/);
    return m ? parseInt(m[1], 10) : 0;
  }

  function currentRest() {
    const m = location.pathname.slice(ctx().length).match(/^\/u\/\d+\/(.*)$/);
    return m && m[1] ? m[1] : "dashboard.html";
  }

  function uUrl(i, rest) {
    return ctx() + "/u/" + i + "/" + String(rest || "").replace(/^\//, "");
  }

  function current() {
    return list()[activeIndex()] || null;
  }

  function token() {
    const a = current();
    return a ? a.token : null;
  }

  function refreshToken() {
    const a = current();
    return a ? a.refreshToken : null;
  }

  function claims() {
    const t = token();
    return t ? decode(t) : {};
  }

  /** Append a new account (or replace an existing same user+tenant); returns its index.
   *  lastLoginAt (optional) is the PREVIOUS login's timestamp from LoginResponse — stored
   *  so the shell footer can show "Last login: ..." for this account's prior session.
   *  refreshTok (optional) is the long-lived refresh token from LoginResponse. */
  function add(tok, lastLoginAt, refreshTok) {
    const c = decode(tok);
    // sub now holds emp_id; the login username rides in its own claim (old tokens: sub == username)
    const a = { token: tok, user: c.username || c.sub, companyCode: c.companyCode, lastLoginAt: lastLoginAt || null, refreshToken: refreshTok || null };
    const arr = list();
    const i = arr.findIndex((x) => x.user === a.user && x.companyCode === a.companyCode);
    if (i >= 0) { arr[i] = a; saveList(arr); return i; }
    arr.push(a); saveList(arr);
    return arr.length - 1;
  }

  /** In-place patch after a silent /api/auth/refresh — preserves lastLoginAt etc. */
  function updateTokens(index, tok, refreshTok) {
    const arr = list();
    if (!arr[index]) return;
    arr[index].token = tok;
    if (refreshTok) arr[index].refreshToken = refreshTok;
    saveList(arr);
  }

  /**
   * Per-account, per-TAB cache (sessionStorage, not localStorage — clears when
   * the tab closes, unlike the account list). Used for the "common on every
   * page" boot data (theme, menu, identity, settings): this app is multi-page
   * (a real navigation, not client-side routing), so without this every single
   * page view would re-fetch all of that from the server. Keyed by account
   * index so switching accounts (a different /u/N) never sees stale data.
   */
  function cacheKey(name) { return "sfa_cache_" + name + "_u" + activeIndex(); }
  function cacheGet(name) {
    try { return JSON.parse(sessionStorage.getItem(cacheKey(name))); }
    catch (e) { return null; }
  }
  function cacheSet(name, value) {
    try { sessionStorage.setItem(cacheKey(name), JSON.stringify(value)); } catch (e) {}
  }
  function cacheClear(name) {
    try { sessionStorage.removeItem(cacheKey(name)); } catch (e) {}
  }

  /** Drop every cached-per-index entry — sign-out reshuffles indices, so a stale
   *  per-index cache could otherwise show the wrong account's data after re-login. */
  function clearAllCache() {
    try {
      Object.keys(sessionStorage)
        .filter((k) => k.indexOf("sfa_cache_") === 0)
        .forEach((k) => sessionStorage.removeItem(k));
    } catch (e) {}
  }

  function signOut(index) {
    const arr = list();
    arr.splice(index, 1);
    clearAllCache();
    if (arr.length) { saveList(arr); location.replace(uUrl(0, "dashboard.html")); }
    else { localStorage.removeItem(KEY); location.replace(ctx() + "/"); }
  }

  function signOutAll() {
    localStorage.removeItem(KEY);
    clearAllCache();
    location.replace(ctx() + "/");
  }

  window.Session = {
    KEY, list, decode, ctx, activeIndex, currentRest, uUrl,
    current, token, refreshToken, claims, add, updateTokens, signOut, signOutAll,
    cache: { get: cacheGet, set: cacheSet, clear: cacheClear },
  };
})(window);
