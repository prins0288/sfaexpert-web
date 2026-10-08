/* ============================================================================
 * Activity — generic, reusable "recent searches" history for report pages.
 * Works for EVERY report the same way: the server scopes everything to the
 * calling user's own username (never client-supplied, never another user's
 * data — see ReportActivityController) and to one reportKey per report page.
 *
 * A report page only needs to:
 *   1. include this script
 *   2. after a successful search, call Activity.record(reportKey, reportLabel,
 *      { ...its own filter values... }, humanReadableSummaryString)
 *   3. add a container element and call Activity.renderInto(containerId,
 *      reportKey, function (savedFilters) { ...populate ITS OWN fields from
 *      savedFilters, then call its own search function... })
 *
 * filters is opaque to this module and to the server — each report defines
 * its own shape; renderInto() just hands back exactly what record() saved.
 *
 * Cached in Session.cache (sessionStorage, per-account, per-tab — same
 * mechanism js/core/theme.js and shell.js use for menu/identity/settings):
 * the FIRST renderInto()/list() for a given reportKey in this tab hits the
 * server; every one after that reads the cache — no repeat network call.
 * record() invalidates that report's cache right after a successful save, so
 * the very next open re-fetches (and re-caches) the now-current list instead
 * of showing something stale.
 * ==========================================================================*/
(function (window) {
  "use strict";

  function cacheKey(reportKey) { return "activity_" + reportKey; }

  async function record(reportKey, reportLabel, filters, summary) {
    try {
      await Api.post(API.reportActivity.save, {
        reportKey, reportLabel, summary,
        filters: JSON.stringify(filters || {}),
      });
      if (window.Session) Session.cache.clear(cacheKey(reportKey));
    } catch (e) { /* best-effort — never block the report the user is looking at */ }
  }

  async function list(reportKey) {
    if (window.Session) {
      const cached = Session.cache.get(cacheKey(reportKey));
      if (cached) return cached;
    }
    try {
      const rows = await Api.get(API.reportActivity.list, { query: { reportKey } });
      if (window.Session) Session.cache.set(cacheKey(reportKey), rows);
      return rows;
    } catch (e) { return []; }
  }

  /**
   * Renders the activity list into #containerId. Each row gets a "Load"
   * button; clicking it parses that row's saved filters and hands them to
   * onLoad(filtersObject) — the report page decides what to do with them
   * (populate its fields, then re-run its own search — this is what makes the
   * report "live load" with the filters auto-selected AND the search already run).
   */
  async function renderInto(containerId, reportKey, onLoad) {
    const el = document.getElementById(containerId);
    if (!el) return;
    const rows = await list(reportKey);
    if (!rows.length) {
      el.innerHTML = '<p class="text-muted small mb-0">No searches yet — run one above and it\'ll show up here.</p>';
      return;
    }
    el.innerHTML = rows.map((r) => `
      <div class="sfa-activity-row">
        <div class="sfa-activity-main">
          <div class="sfa-activity-summary">${SFA.esc(r.summary || "Search")}</div>
          <div class="sfa-activity-time"><i class="bi bi-clock-history"></i> ${SFA.esc(r.createdAt || "")}</div>
        </div>
        <button type="button" class="btn btn-sm btn-outline-primary" data-activity-oid="${r.oid}">
          <i class="bi bi-arrow-repeat"></i> Load
        </button>
      </div>`).join("");
    el.querySelectorAll("[data-activity-oid]").forEach((btn) => {
      btn.addEventListener("click", () => {
        const row = rows.find((r) => String(r.oid) === btn.getAttribute("data-activity-oid"));
        if (!row) return;
        let filters = {};
        try { filters = JSON.parse(row.filters) || {}; } catch (e) {}
        onLoad(filters);
      });
    });
  }

  window.Activity = { record, list, renderInto };
})(window);
