/* ============================================================================
 * Theme — the dynamic, per-user appearance engine.
 *
 * The server resolves (mode default -> preset -> user override) and returns a
 * ThemeConfig; this module applies it by writing every token as a CSS custom
 * property (--sfa-*) on <html>, plus data-theme / data-nav / data-density
 * attributes. Nothing is hard-coded — change a token here and the whole UI
 * (sidebar, header, footer, cards, tables, buttons...) recolours instantly.
 *
 * Flow:
 *   1. applyCached()  — paint the last-known theme immediately (no flash)
 *   2. load()         — fetch the authoritative config and re-apply
 *   3. customizer     — live-preview on input, persist on change (per user, DB)
 * ==========================================================================*/
(function (window) {
  "use strict";

  /**
   * The theme is PER TENANT, and the browser can hold several tenants signed in
   * at once (/u/0, /u/1, ...). These cache keys must therefore be scoped to the
   * account slot in the URL — with one shared "sfa_theme" key the instant-paint
   * on boot would show whichever tenant was customised LAST, on every tenant.
   * The inline boot script at the top of every page derives the same suffix.
   */
  function acctSuffix() {
    const m = location.pathname.match(/^\/u\/(\d+)(\/|$)/);
    return "_u" + (m ? m[1] : "0");
  }
  const SFX = acctSuffix();
  const CACHE = "sfa_theme" + SFX;    // full ThemeConfig JSON
  const K_MODE = "sfa_mode" + SFX;
  const K_NAV = "sfa_nav" + SFX;
  const K_DENSITY = "sfa_density" + SFX;
  const K_FONT = "sfa_font" + SFX;

  let state = null;                    // current ThemeConfig
  const listeners = [];

  function readCache() {
    try { return JSON.parse(localStorage.getItem(CACHE)); } catch (e) { return null; }
  }
  function writeCache(cfg) {
    try {
      localStorage.setItem(CACHE, JSON.stringify(cfg));
      localStorage.setItem(K_MODE, cfg.mode);
      localStorage.setItem(K_NAV, cfg.navLayout);
      localStorage.setItem(K_DENSITY, cfg.density);
      localStorage.setItem(K_FONT, cfg.fontScale);
    } catch (e) { /* storage full / disabled — non-fatal */ }
  }

  /** Write one mode's resolved tokens as inline CSS variables on <html>. */
  function paintTokens(cfg) {
    const root = document.documentElement;
    const tokens = (cfg.resolved && cfg.resolved[cfg.mode]) || {};
    Object.keys(tokens).forEach((key) => {
      root.style.setProperty("--sfa-" + key, tokens[key]);
    });
    root.setAttribute("data-theme", cfg.mode);
    root.setAttribute("data-nav", cfg.navLayout);
    root.setAttribute("data-density", cfg.density);
    root.style.setProperty("--sfa-font-scale", cfg.fontScale);
  }

  function apply(cfg) {
    if (!cfg) return;
    state = cfg;
    paintTokens(cfg);
    writeCache(cfg);
    listeners.forEach((fn) => { try { fn(cfg); } catch (e) { /* ignore */ } });
  }

  const Theme = {
    get state() { return state; },
    onChange(fn) { listeners.push(fn); },

    /** Paint whatever we cached last time — instant, before any network call. */
    applyCached() {
      const cfg = readCache();
      if (cfg) paintTokens(cfg), (state = cfg);
      return cfg;
    },

    /**
     * Fetch the authoritative config and apply it — UNLESS this account already
     * has one cached for this browser tab (Session.cache, sessionStorage), in
     * which case that's used and NO network request is made at all. This app is
     * multi-page (full navigations, not client-side routing), so without this
     * every single page view would re-fetch theme/menu/identity/settings from
     * the server; the cache clears itself when the tab closes or the account
     * signs out, and save()/resetMode() below keep it fresh on every change.
     */
    async load() {
      const cached = window.Session && Session.cache.get("theme");
      if (cached) { apply(cached); return cached; }
      const cfg = await Api.get(API.theme.get);
      apply(cfg);
      if (window.Session) Session.cache.set("theme", cfg);
      return cfg;
    },

    /** Persist a partial change ({navLayout|mode|preset|density|fontScale|tokens}). */
    async save(patch) {
      const cfg = await Api.put(API.theme.update, patch);
      const layoutChanged = state && cfg.navLayout !== state.navLayout;
      apply(cfg);
      if (window.Session) Session.cache.set("theme", cfg);
      // Vertical<->horizontal is a structural chrome change; rebuild cleanly.
      if (layoutChanged && window.Shell && typeof Shell.rebuild === "function") {
        Shell.rebuild();
      }
      return cfg;
    },

    /** Live, un-persisted preview of a single token (used while dragging). */
    preview(key, value) {
      document.documentElement.style.setProperty("--sfa-" + key, value);
    },

    async setMode(mode) { return this.save({ mode }); },
    async setLayout(navLayout) { return this.save({ navLayout }); },
    async setDensity(density) { return this.save({ density }); },
    async setFontScale(fontScale) { return this.save({ fontScale }); },
    async setPreset(preset) { return this.save({ preset }); },
    async toggleMode() { return this.setMode(state && state.mode === "dark" ? "light" : "dark"); },

    /** Persist one token override for the active mode. */
    async saveToken(key, value) {
      const tokens = {}; tokens[key] = value;
      return this.save({ mode: state.mode, tokens });
    },

    async resetMode(mode) {
      const cfg = await Api.post(API.theme.reset, null, { query: { mode: mode || state.mode } });
      apply(cfg);
      if (window.Session) Session.cache.set("theme", cfg);
      return cfg;
    },

    buildCustomizer,
  };

  // --------------------------------------------------------------------------
  // Customizer UI (rendered into the offcanvas the shell provides)
  // --------------------------------------------------------------------------
  const esc = (s) => (s == null ? "" : String(s)
    .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;"));

  function segmented(name, value, options) {
    return `<div class="sfa-seg" role="group">` + options.map((o) =>
      `<button type="button" class="sfa-seg-btn ${o.value === value ? "active" : ""}"
        data-seg="${name}" data-value="${o.value}">${esc(o.label)}</button>`).join("") + `</div>`;
  }

  function tokenRow(t, mode) {
    const value = mode === "dark" ? t.dark : t.light;
    const overridden = mode === "dark" ? t.darkOverridden : t.lightOverridden;
    const control = t.type === "color"
      ? `<span class="sfa-swatch"><input type="color" class="sfa-token-color" data-key="${t.key}"
             value="${/^#([0-9a-f]{6})$/i.test(value) ? value : "#000000"}" title="${esc(value)}" /></span>
         <input type="text" class="form-control form-control-sm sfa-token-text" data-key="${t.key}" value="${esc(value)}" />`
      : `<input type="text" class="form-control form-control-sm sfa-token-text" data-key="${t.key}" value="${esc(value)}" />`;
    return `<div class="sfa-token-row ${overridden ? "is-overridden" : ""}">
        <label class="sfa-token-label" title="--sfa-${t.key}">${esc(t.label)}</label>
        <div class="sfa-token-control">${control}
          <button type="button" class="sfa-token-reset" data-key="${t.key}" title="Reset to default">&times;</button>
        </div>
      </div>`;
  }

  function buildCustomizer(container) {
    if (!state) return;
    const cfg = state;
    const presetBtns = cfg.presets.map((p) =>
      `<button type="button" class="sfa-preset ${p.key === cfg.preset ? "active" : ""}" data-preset="${p.key}">${esc(p.label)}</button>`
    ).join("");

    const groups = cfg.groups.map((g, i) => `
      <div class="sfa-cz-group">
        <button type="button" class="sfa-cz-group-head" data-acc="${i}">
          <span>${esc(g.label)}</span><i class="bi bi-chevron-down"></i>
        </button>
        <div class="sfa-cz-group-body" data-acc-body="${i}" ${i === 0 ? "" : "hidden"}>
          ${g.tokens.map((t) => tokenRow(t, cfg.mode)).join("")}
        </div>
      </div>`).join("");

    container.innerHTML = `
      <div class="sfa-cz-section">
        <div class="sfa-cz-label">Mode</div>
        ${segmented("mode", cfg.mode, [{ value: "light", label: "☀ Light" }, { value: "dark", label: "🌙 Dark" }])}
      </div>
      <div class="sfa-cz-section">
        <div class="sfa-cz-label">Navigation layout</div>
        ${segmented("navLayout", cfg.navLayout, [{ value: "vertical", label: "▤ Vertical" }, { value: "horizontal", label: "▔ Horizontal" }])}
      </div>
      <div class="sfa-cz-section">
        <div class="sfa-cz-label">Density</div>
        ${segmented("density", cfg.density, [{ value: "comfortable", label: "Comfortable" }, { value: "compact", label: "Compact" }])}
      </div>
      <div class="sfa-cz-section">
        <div class="sfa-cz-label">Base font size <span class="sfa-cz-val" id="czFontVal">${esc(cfg.fontScale)}×</span></div>
        <input type="range" class="form-range" id="czFont" min="0.85" max="1.30" step="0.05" value="${esc(cfg.fontScale)}" />
      </div>
      <div class="sfa-cz-section">
        <div class="sfa-cz-label">Preset</div>
        <div class="sfa-preset-row">${presetBtns}</div>
      </div>
      <div class="sfa-cz-section">
        <div class="sfa-cz-label d-flex justify-content-between align-items-center">
          <span>Colours &amp; sizes <small class="text-muted">(${esc(cfg.mode)})</small></span>
          <button type="button" class="btn btn-sm btn-link p-0 sfa-cz-reset">Reset ${esc(cfg.mode)}</button>
        </div>
        <div class="sfa-cz-groups">${groups}</div>
      </div>`;

    wireCustomizer(container);
  }

  function wireCustomizer(container) {
    // segmented buttons (mode / layout / density)
    container.querySelectorAll(".sfa-seg-btn").forEach((btn) => {
      btn.addEventListener("click", async () => {
        const name = btn.getAttribute("data-seg");
        const value = btn.getAttribute("data-value");
        btn.parentElement.querySelectorAll(".sfa-seg-btn").forEach((b) => b.classList.remove("active"));
        btn.classList.add("active");
        try {
          await Theme.save({ [name]: value });
          if (name === "mode") buildCustomizer(container);   // re-render token values for new mode
        } catch (e) { window.SFA && SFA.toast(e.message, false); }
      });
    });

    // preset buttons
    container.querySelectorAll(".sfa-preset").forEach((btn) => {
      btn.addEventListener("click", async () => {
        container.querySelectorAll(".sfa-preset").forEach((b) => b.classList.remove("active"));
        btn.classList.add("active");
        try { await Theme.setPreset(btn.getAttribute("data-preset")); buildCustomizer(container); }
        catch (e) { window.SFA && SFA.toast(e.message, false); }
      });
    });

    // font range: live preview on input, save on change
    const font = container.querySelector("#czFont");
    const fontVal = container.querySelector("#czFontVal");
    if (font) {
      font.addEventListener("input", () => {
        document.documentElement.style.setProperty("--sfa-font-scale", font.value);
        fontVal.textContent = font.value + "×";
      });
      font.addEventListener("change", () => Theme.setFontScale(font.value));
    }

    // accordion
    container.querySelectorAll(".sfa-cz-group-head").forEach((head) => {
      head.addEventListener("click", () => {
        const body = container.querySelector(`[data-acc-body="${head.getAttribute("data-acc")}"]`);
        if (body) body.hidden = !body.hidden;
        head.classList.toggle("open", body && !body.hidden);
      });
    });

    // token colour/text inputs: live preview, persist on change; keep the pair in sync
    function bindToken(key) {
      const color = container.querySelector(`.sfa-token-color[data-key="${key}"]`);
      const text = container.querySelector(`.sfa-token-text[data-key="${key}"]`);
      const row = text.closest(".sfa-token-row");
      const preview = (v) => Theme.preview(key, v);
      const persist = (v) => Theme.saveToken(key, v).then(() => row.classList.add("is-overridden"))
        .catch((e) => window.SFA && SFA.toast(e.message, false));
      if (color) {
        color.addEventListener("input", () => { text.value = color.value; preview(color.value); });
        color.addEventListener("change", () => persist(color.value));
      }
      if (text) {
        text.addEventListener("input", () => { preview(text.value); if (color && /^#([0-9a-f]{6})$/i.test(text.value)) color.value = text.value; });
        text.addEventListener("change", () => persist(text.value));
      }
    }
    container.querySelectorAll(".sfa-token-text").forEach((el) => bindToken(el.getAttribute("data-key")));

    // per-token reset: clear the override (blank value) -> falls back to default
    container.querySelectorAll(".sfa-token-reset").forEach((btn) => {
      btn.addEventListener("click", async () => {
        const key = btn.getAttribute("data-key");
        try { await Theme.saveToken(key, ""); buildCustomizer(container); }
        catch (e) { window.SFA && SFA.toast(e.message, false); }
      });
    });

    // reset the whole mode
    const resetBtn = container.querySelector(".sfa-cz-reset");
    if (resetBtn) resetBtn.addEventListener("click", async () => {
      if (!confirm("Reset all " + state.mode + " colours to their defaults?")) return;
      try { await Theme.resetMode(state.mode); buildCustomizer(container); }
      catch (e) { window.SFA && SFA.toast(e.message, false); }
    });
  }

  window.Theme = Theme;
})(window);
