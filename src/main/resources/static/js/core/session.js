/* ============================================================================
 * Session — the Gmail-style multi-account JWT store (/u/N URLs).
 *
 * One JWT per signed-in account kept in localStorage; the /u/{index}/ URL
 * segment selects which account is active. This is the SINGLE definition of the
 * account store — the login page and the app shell both use it (previously the
 * logic was copy-pasted in two places).
 * ==========================================================================*/
(function (window) {
  "use strict";

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

  function activeIndex() {
    const m = location.pathname.match(/^\/u\/(\d+)(\/|$)/);
    return m ? parseInt(m[1], 10) : 0;
  }

  function currentRest() {
    const m = location.pathname.match(/^\/u\/\d+\/(.*)$/);
    return m && m[1] ? m[1] : "dashboard.html";
  }

  function uUrl(i, rest) {
    return "/u/" + i + "/" + String(rest || "").replace(/^\//, "");
  }

  function current() {
    return list()[activeIndex()] || null;
  }

  function token() {
    const a = current();
    return a ? a.token : null;
  }

  function claims() {
    const t = token();
    return t ? decode(t) : {};
  }

  /** Append a new account (or replace an existing same user+tenant); returns its index. */
  function add(tok) {
    const c = decode(tok);
    const a = { token: tok, user: c.sub, tenant: c.tenant };
    const arr = list();
    const i = arr.findIndex((x) => x.user === a.user && x.tenant === a.tenant);
    if (i >= 0) { arr[i] = a; saveList(arr); return i; }
    arr.push(a); saveList(arr);
    return arr.length - 1;
  }

  function signOut(index) {
    const arr = list();
    arr.splice(index, 1);
    if (arr.length) { saveList(arr); location.replace(uUrl(0, "dashboard.html")); }
    else { localStorage.removeItem(KEY); location.replace("/"); }
  }

  function signOutAll() {
    localStorage.removeItem(KEY);
    location.replace("/");
  }

  window.Session = {
    KEY, list, decode, activeIndex, currentRest, uUrl,
    current, token, claims, add, signOut, signOutAll,
  };
})(window);
