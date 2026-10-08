/* ============================================================================
 * Api — the global HTTP client. ONE place that talks to the server, the way a
 * React app has a single axios/fetch service. Every screen calls these; no
 * screen builds its own fetch() or sets its own headers.
 *
 *   Api.get(API.master.route.list)
 *   Api.post(API.master.route.save, body)
 *   Api.post(API.master.route.status(oid), null, { query: { status: 'N' } })
 *   Api.put(API.theme.update, themePatch)
 *   Api.del(path)
 *   Api.upload(API.master.route.upload, file)        // multipart
 *   Api.download(API.master.route.template, 'route_template.xlsx')
 *
 * Cross-cutting concerns handled here, once:
 *   - Bearer token from the active account (Session)
 *   - JSON encode/decode
 *   - query-string building (no manual '?status=' concatenation)
 *   - 401 -> silently exchange the account's refresh token for a new access
 *     token (see tryRefresh) and retry the request ONCE; only if that also
 *     fails (refresh token expired/revoked/missing) do we sign out and bounce
 *     to login. Concurrent 401s share a single in-flight refresh call.
 *   - errors -> thrown as Error(message) using the server's error body
 * ==========================================================================*/
(function (window) {
  "use strict";

  function authHeader(extra) {
    const t = window.Session ? Session.token() : null;
    return Object.assign(t ? { Authorization: "Bearer " + t } : {}, extra || {});
  }

  /** Prepend the deployment context prefix (e.g. "/sfaexpert" behind a reverse
   *  proxy) to a root-absolute API path, so requests route through the proxy.
   *  Absolute http(s) URLs are left untouched. */
  function apiUrl(path) {
    const ctx = window.Session ? Session.ctx() : "";
    return (ctx && typeof path === "string" && path.charAt(0) === "/") ? ctx + path : path;
  }

  function withQuery(path, query) {
    if (!query) return path;
    const qs = Object.entries(query)
      .filter(([, v]) => v !== undefined && v !== null)
      .map(([k, v]) => encodeURIComponent(k) + "=" + encodeURIComponent(v))
      .join("&");
    if (!qs) return path;
    return path + (path.includes("?") ? "&" : "?") + qs;
  }

  function onUnauthorized() {
    if (window.Session) Session.signOut(Session.activeIndex());
    else location.replace("/");   // no Session -> can't know the prefix; root is best-effort
  }

  // Concurrent 401s (e.g. several parallel requests on page load) must not each
  // fire their own /api/auth/refresh — they share an in-flight promise. But that
  // promise MUST be keyed by account index: several tenants can be signed in at
  // once (/u/0, /u/1...), and if account B piggy-backed on account A's refresh
  // it would end up authenticated AS A — a cross-tenant identity leak (B's write
  // landing in A's database). Each account gets its own in-flight slot, and the
  // index is captured up front so the new token is written to the RIGHT account
  // even if the active /u/N changes while the refresh is in flight.
  const refreshInFlight = {};

  /** Exchanges the given (default active) account's refresh token for a new access token. */
  function tryRefresh(index) {
    const idx = index != null ? index : (window.Session ? Session.activeIndex() : 0);
    if (refreshInFlight[idx]) return refreshInFlight[idx];
    refreshInFlight[idx] = (async () => {
      const acct = window.Session ? Session.list()[idx] : null;
      const rt = acct ? acct.refreshToken : null;
      if (!rt) return false;
      try {
        const res = await fetch(apiUrl(window.API.auth.refresh), {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ refreshToken: rt }),
        });
        if (!res.ok) return false;
        const raw = await res.json();
        // unwrap the { success, data } envelope (refresh payload is under .data)
        const payload = (raw && typeof raw === "object" && raw.data) ? raw.data : raw;
        Session.updateTokens(idx, payload.token, payload.refreshToken);
        return true;
      } catch (e) {
        return false;
      } finally {
        delete refreshInFlight[idx];
      }
    })();
    return refreshInFlight[idx];
  }

  // Wrapper: drive the global page loader (js/core/uikit.js) around every call.
  // Pass { noLoader: true } to opt a specific request out (e.g. silent polls).
  async function request(method, path, body, opts) {
    opts = opts || {};
    var showLoader = !opts.noLoader && window.SFALoader;
    if (showLoader) SFALoader.show();
    try { return await _request(method, path, body, opts); }
    finally { if (showLoader) SFALoader.hide(); }
  }

  async function _request(method, path, body, opts) {
    opts = opts || {};
    const url = apiUrl(withQuery(path, opts.query));

    const init = { method, headers: authHeader(opts.headers) };

    if (body instanceof FormData) {
      init.body = body;                       // browser sets multipart boundary
    } else if (body !== undefined && body !== null) {
      init.headers["Content-Type"] = "application/json";
      init.body = typeof body === "string" ? body : JSON.stringify(body);
    }

    let res;
    try {
      res = await fetch(url, init);
      // ANY response — even a 4xx/5xx — proves the server is reachable, so the
      // footer's online/offline indicator (js/shell.js) doesn't have to wait
      // for its own periodic ping to notice a Tomcat restart has finished.
      if (window.sfaReportNetworkOk) sfaReportNetworkOk();
    } catch (netErr) {
      // fetch() itself throwing means the server could NOT be reached at all
      // (connection refused, e.g. mid Tomcat restart) — tell the indicator
      // immediately instead of waiting up to one poll interval.
      if (window.sfaReportNetworkError) sfaReportNetworkError();
      throw new Error("Network error: " + netErr.message);
    }

    // 401: try a silent refresh-token exchange first (once per request), then
    // retry with the new access token. Public calls (e.g. the login request
    // itself) pass noAuthRedirect so a "bad credentials" 401 surfaces as a
    // normal error instead of triggering any of this.
    if (res.status === 401 && !opts.noAuthRedirect) {
      if (!opts._retried && await tryRefresh()) {
        return request(method, path, body, Object.assign({}, opts, { _retried: true }));
      }
      onUnauthorized();
      throw new Error("Session expired. Please sign in again.");
    }

    if (res.status === 204) return null;

    const isJson = (res.headers.get("content-type") || "").includes("application/json");
    const data = isJson ? await res.json().catch(() => ({})) : await res.text();

    if (!res.ok) {
      const msg = (data && (data.message || data.error)) || ("HTTP " + res.status);
      const err = new Error(msg);
      err.status = res.status;
      err.body = data;
      err.errors = data && data.errors;   // per-field validation errors, if any
      throw err;
    }
    // Unwrap the standard { success, status, message, data } envelope so every page
    // keeps reading the payload directly (res.field), not res.data.field. Anything
    // not shaped like the envelope is returned as-is.
    if (data && typeof data === "object"
        && typeof data.success === "boolean" && typeof data.status === "number" && "data" in data) {
      return data.data;
    }
    return data;
  }

  const Api = {
    get: (path, opts) => request("GET", path, null, opts),
    post: (path, body, opts) => request("POST", path, body, opts),
    put: (path, body, opts) => request("PUT", path, body, opts),
    patch: (path, body, opts) => request("PATCH", path, body, opts),
    del: (path, opts) => request("DELETE", path, null, opts),

    /** multipart upload of a single file under field name `field` (default "file"). */
    upload(path, file, field, opts) {
      const fd = new FormData();
      fd.append(field || "file", file);
      return request("POST", path, fd, opts);
    },

    /** Authenticated file download that saves as `filename`. */
    async download(path, filename, opts) {
      opts = opts || {};
      let res = await fetch(apiUrl(withQuery(path, opts.query)), { headers: authHeader(opts.headers) });
      if (res.status === 401 && await tryRefresh()) {
        res = await fetch(apiUrl(withQuery(path, opts.query)), { headers: authHeader(opts.headers) });
      }
      if (res.status === 401) { onUnauthorized(); return; }
      if (!res.ok) throw new Error("HTTP " + res.status);
      const blob = await res.blob();
      const href = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = href;
      a.download = filename || "download";
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(href);
    },
  };

  window.Api = Api;
})(window);
