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
 *   - 401  -> sign the account out and bounce to login
 *   - errors -> thrown as Error(message) using the server's error body
 * ==========================================================================*/
(function (window) {
  "use strict";

  function authHeader(extra) {
    const t = window.Session ? Session.token() : null;
    return Object.assign(t ? { Authorization: "Bearer " + t } : {}, extra || {});
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
    else location.replace("/");
  }

  async function request(method, path, body, opts) {
    opts = opts || {};
    const url = withQuery(path, opts.query);

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
    } catch (netErr) {
      throw new Error("Network error: " + netErr.message);
    }

    // 401 normally means an expired session -> sign out and bounce to login.
    // Public calls (e.g. the login request itself) pass noAuthRedirect so a
    // "bad credentials" 401 surfaces as a normal error instead of a redirect.
    if (res.status === 401 && !opts.noAuthRedirect) {
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
      throw err;
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
      const res = await fetch(withQuery(path, opts.query), { headers: authHeader(opts.headers) });
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
