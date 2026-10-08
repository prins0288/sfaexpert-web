/* ============================================================================
 * Settings — company-wide settings (currently just dateFormat), fetched once
 * from /api/settings and cached. Every date picker (Flatpickr, see app.js's
 * sfaFlatpickr) and every report date column reads the format from here, so
 * changing it on the Settings page (settings/general.html) takes effect
 * everywhere without touching individual pages.
 *
 * Format tokens used everywhere in THIS app (not Flatpickr's own tokens):
 *   dd   = 2-digit day
 *   MM   = 2-digit month
 *   yyyy = 4-digit year
 * e.g. "dd-MM-yyyy", "MM-dd-yyyy", "yyyy-MM-dd", "dd/MM/yyyy".
 * ==========================================================================*/
(function (window) {
  "use strict";

  const CACHE_KEY = "sfa_settings";
  const DEFAULT_DATE_FORMAT = "dd-MM-yyyy";

  let SETTINGS = null;
  try { SETTINGS = JSON.parse(localStorage.getItem(CACHE_KEY)); } catch (e) { SETTINGS = null; }

  /** force=true bypasses the session cache (used after Save on the Settings page). */
  async function load(force) {
    if (!force && window.Session) {
      const cached = Session.cache.get("settings");
      if (cached) { SETTINGS = cached; return SETTINGS; }
    }
    try {
      SETTINGS = await Api.get(API.settings.get);
      if (window.Session) Session.cache.set("settings", SETTINGS);
      try { localStorage.setItem(CACHE_KEY, JSON.stringify(SETTINGS)); } catch (e) {}
    } catch (e) {
      // offline / not migrated yet: keep whatever was cached (or defaults)
    }
    if (!SETTINGS || !SETTINGS.dateFormat) SETTINGS = Object.assign({ dateFormat: DEFAULT_DATE_FORMAT }, SETTINGS || {});
    return SETTINGS;
  }

  function dateFormat() {
    return (SETTINGS && SETTINGS.dateFormat) || DEFAULT_DATE_FORMAT;
  }

  /** The single "Content Protection" toggle — see js/app.js's sfaApplyContentProtection(). */
  function contentProtectionEnabled() {
    return !!(SETTINGS && SETTINGS.contentProtection === "Y");
  }

  /** Our "dd-MM-yyyy" style token string -> Flatpickr's own "d-m-Y" token string. */
  function flatpickrFormat(fmt) {
    return (fmt || dateFormat()).replace(/yyyy/g, "Y").replace(/MM/g, "m").replace(/dd/g, "d");
  }

  /** Formats a Date / ISO string ("2026-07-30" or full timestamp) per the configured format. Empty string on bad input. */
  function formatDate(value, fmt) {
    if (!value) return "";
    const d = value instanceof Date ? value : new Date(value);
    if (isNaN(d.getTime())) return "";
    const p = (n) => String(n).padStart(2, "0");
    const parts = { dd: p(d.getDate()), MM: p(d.getMonth() + 1), yyyy: String(d.getFullYear()) };
    return (fmt || dateFormat()).replace(/dd|MM|yyyy/g, (m) => parts[m]);
  }

  window.Settings = { load, dateFormat, flatpickrFormat, formatDate, contentProtectionEnabled, raw: () => SETTINGS };
})(window);
