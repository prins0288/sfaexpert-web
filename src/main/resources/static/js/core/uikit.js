/* ============================================================================
 * UIKit — global, generic keyboard + UX layer. ONE file, auto-applied on every
 * page (loaded before shell.js; Shell calls UIKit.init() after it builds the
 * chrome and runs pageInit, and again on Shell.rebuild()).
 *
 * Nothing here is page-specific: every action is discovered from the DOM by
 * ROLE — a button is found by its visible label/icon (every master uses the
 * same "Add Single" / "Add Multiple" / "Bulk Upload" labels and the same
 * bi-pencil / bi-slash-circle row icons, even though each page's own JS names
 * its handlers differently), the filter is the card whose header carries the
 * funnel icon, the grid is the DataTable. So a new screen gets all of this for
 * free, no wiring.
 *
 * Keyboard (only when you are NOT typing in a field, and no modal is open):
 *   s            focus the top menu search
 *   f            toggle the Filters card (and focus its first field) — or, on a
 *                master grid with no filter card, open the column-filter row
 *   r            Reset
 *   n            New (Add Single)
 *   e            Edit the highlighted row
 *   d            Delete / deactivate the highlighted row
 *   Alt+M        Add Multiple
 *   Alt+U        Bulk Upload
 *   ?            open the page Help popover
 *   ArrowUp/Down move the row highlight through the visible grid rows
 * Inside the Filters card:
 *   Enter        jump to the next field (opening a <select>'s / date field's
 *                picker as it lands); on the last field it runs Search
 * In any grid:
 *   Shift+click  on a row checkbox selects the whole range since the last one
 * ==========================================================================*/
(function (window) {
  "use strict";

  var bound = false;   // document-level listeners are bound exactly once

  // ---- small DOM helpers --------------------------------------------------
  function isTyping(el) {
    return !!(el && el.closest && el.closest('input, textarea, select, [contenteditable="true"]'));
  }
  function modalOpen() { return !!document.querySelector('.modal.show'); }
  function visible(el) { return !!(el && el.offsetParent !== null); }

  /** Click the first VISIBLE <button>/a.btn whose text contains any of `labels` (lower-case). */
  function clickByLabel(labels) {
    var btns = document.querySelectorAll('button, a.btn');
    for (var i = 0; i < btns.length; i++) {
      if (!visible(btns[i])) continue;
      var t = btns[i].textContent.trim().toLowerCase();
      for (var j = 0; j < labels.length; j++) {
        if (t.indexOf(labels[j]) !== -1) { btns[i].click(); return true; }
      }
    }
    return false;
  }

  // ---- the Filters card (report-style) ------------------------------------
  /** The filter card = a .card whose header carries the funnel icon. */
  function filterCard() {
    var heads = document.querySelectorAll('.card > .card-header');
    for (var i = 0; i < heads.length; i++) {
      if (heads[i].querySelector('.bi-funnel') && visible(heads[i])) return heads[i].closest('.card');
    }
    return null;
  }

  function cardBody(card) { return card ? card.querySelector('.card-body') : null; }

  function pageKey() {
    var app = document.getElementById('app');
    return 'sfa_filter_' + ((app && app.getAttribute('data-page')) || location.pathname);
  }

  /** Add the collapse chevron to a filter card's header once, and restore its
   *  last state (per page, remembered in localStorage). */
  function enhanceFilterCard(card) {
    var head = card.querySelector('.card-header');
    if (!head || head.querySelector('.sfa-filter-toggle')) return;   // already done
    head.classList.add('sfa-filter-head');
    var btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'sfa-filter-toggle';
    btn.title = 'Hide / show filters (f)';
    btn.innerHTML = '<i class="bi bi-chevron-up"></i>';
    // a .card-tools block floats to the right; sit inside it if present, else on the header
    (head.querySelector('.card-tools') || head).appendChild(btn);
    btn.addEventListener('click', function (e) { e.preventDefault(); e.stopPropagation(); toggleFilterCard(card); });
    // header click (outside the buttons) also toggles
    head.addEventListener('click', function (e) {
      if (e.target.closest('button, a, .card-tools')) return;
      toggleFilterCard(card);
    });
  }

  function isCollapsed(card) { return card.classList.contains('sfa-filter-collapsed'); }

  function setCollapsed(card, collapsed) {
    var body = cardBody(card);
    card.classList.toggle('sfa-filter-collapsed', collapsed);
    if (body) body.hidden = collapsed;
    var icon = card.querySelector('.sfa-filter-toggle i');
    if (icon) icon.className = collapsed ? 'bi bi-chevron-down' : 'bi bi-chevron-up';
    try { localStorage.setItem(pageKey(), collapsed ? '1' : '0'); } catch (e) {}
  }

  function toggleFilterCard(card) {
    var willOpen = isCollapsed(card);
    setCollapsed(card, !willOpen);
    if (willOpen) focusControl(filterControls(card)[0]);   // opening -> jump into the first field
  }

  // ---- Filters: Enter jumps field-to-field --------------------------------
  /** Ordered, visible, user-editable controls inside a filter card. */
  function filterControls(card) {
    if (!card) return [];
    var all = card.querySelectorAll('select, textarea, input:not([type=hidden]):not([type=checkbox]):not([type=radio])');
    var out = [];
    all.forEach(function (el) {
      // flatpickr hides the real .sfa-date input and shows an altInput clone;
      // skip the hidden original so we land on the visible one only.
      if (!visible(el)) return;
      out.push(el);
    });
    return out;
  }

  function focusControl(el) {
    if (!el) return;
    // a searchable (Select2) select opens its own dropdown instead of showPicker
    if (window.jQuery && window.jQuery(el).hasClass('select2-hidden-accessible')) {
      window.jQuery(el).select2('open');
      return;
    }
    el.focus();
    // open the picker as we arrive: native <select> (Chrome showPicker), and
    // flatpickr date fields open themselves on focus.
    if (el.tagName === 'SELECT' && typeof el.showPicker === 'function') {
      try { el.showPicker(); } catch (e) {}
    }
  }

  function isFilterControl(el) {
    if (!el || !el.matches) return false;
    // let Select2's own search box handle its Enter (option pick) — don't hijack it
    if (el.closest('.select2-container') || el.classList.contains('select2-search__field')) return false;
    if (!el.matches('select, textarea, input:not([type=hidden]):not([type=checkbox]):not([type=radio])')) return false;
    var card = el.closest('.card');
    return !!(card && card.querySelector('.card-header .bi-funnel'));
  }

  function handleFilterEnter(el) {
    var card = el.closest('.card');
    var controls = filterControls(card);
    var i = controls.indexOf(el);
    if (i === -1) return;
    if (i >= controls.length - 1) {
      // last field -> run the search (the card's primary/Search button)
      var btn = card.querySelector('.btn-primary') ||
        Array.prototype.find.call(card.querySelectorAll('button'), function (b) {
          return b.textContent.trim().toLowerCase().indexOf('search') !== -1;
        });
      if (btn) btn.click();
    } else {
      focusControl(controls[i + 1]);
    }
  }

  // ---- the grid: row highlight + arrow navigation -------------------------
  /** The primary data table's currently-VISIBLE body rows (paging/scroll aware). */
  function gridRows() {
    var tbl = document.querySelector('.dataTables_scrollBody table') ||
      document.querySelector('table.dataTable') ||
      document.querySelector('.table-responsive table');
    if (!tbl) return [];
    return Array.prototype.filter.call(tbl.querySelectorAll('tbody tr'), function (r) {
      return visible(r) && !r.classList.contains('dataTables_empty');
    });
  }

  function selectedRow() { return document.querySelector('tbody tr.sfa-row-selected'); }

  function moveRow(dir) {
    var rows = gridRows();
    if (!rows.length) return;
    var cur = selectedRow();
    var idx = cur ? rows.indexOf(cur) : -1;
    var next = idx === -1 ? rows[0] : rows[Math.max(0, Math.min(rows.length - 1, idx + dir))];
    rows.forEach(function (r) { r.classList.remove('sfa-row-selected'); });
    next.classList.add('sfa-row-selected');
    next.scrollIntoView({ block: 'nearest' });
  }

  /** Click a row-scoped action button (by its icon) inside the highlighted row. */
  function rowAction(iconSelector) {
    var row = selectedRow();
    if (!row) return false;
    var icon = row.querySelector(iconSelector);
    var btn = icon && icon.closest('button, a');
    if (btn && !btn.hidden) { btn.click(); return true; }   // hidden = no permission (data-perm)
    return false;
  }

  // ---- top menu search / help --------------------------------------------
  function focusMenuSearch() {
    var s = document.getElementById('sfaSearch');
    if (s) { s.focus(); s.select && s.select(); }
  }

  /** Plain "s" focuses the GRID's own search box (DataTables) — the report/master
   *  table search. Falls back to the top menu search on a page with no grid. */
  function focusGridSearch() {
    var s = document.querySelector('.dataTables_filter input');
    if (s && visible(s)) { s.focus(); s.select && s.select(); return; }
    focusMenuSearch();
  }

  /** Plain "f" opens the grid's column-filter row (the DataTables "Filter" button). */
  function openGridFilter() { clickByLabel(['filter']); }

  function toggleHelp() {
    var btn = document.getElementById('sfaHelpBtn');
    if (!btn) return;
    if (window.bootstrap && bootstrap.Popover) {
      var p = bootstrap.Popover.getInstance(btn);
      if (p) { p.toggle(); return; }
    }
    btn.focus();
  }

  // ---- filter opener (report card, else the grid's column-filter row) -----
  function toggleFilter() {
    var card = filterCard();
    if (card) { toggleFilterCard(card); return; }
    clickByLabel(['filter']);   // master grid: DataTables "Filter" toolbar button
  }

  // ---- checkbox range select (Shift+click) --------------------------------
  var lastCheckbox = null;
  function onDocClick(e) {
    var cb = e.target;
    if (!(cb && cb.matches && cb.matches('tbody input[type="checkbox"]'))) return;
    if (e.shiftKey && lastCheckbox && lastCheckbox !== cb) {
      var body = cb.closest('tbody');
      if (body && lastCheckbox.closest('tbody') === body) {
        var boxes = Array.prototype.slice.call(body.querySelectorAll('input[type="checkbox"]'));
        var a = boxes.indexOf(lastCheckbox), b = boxes.indexOf(cb);
        if (a > -1 && b > -1) {
          for (var i = Math.min(a, b); i <= Math.max(a, b); i++) {
            if (boxes[i].checked !== cb.checked) {
              boxes[i].checked = cb.checked;
              boxes[i].dispatchEvent(new Event('change', { bubbles: true }));
            }
          }
        }
      }
    }
    lastCheckbox = cb;
  }

  // ---- the single global key handler --------------------------------------
  function onKeyDown(e) {
    var el = e.target;

    // 0) Escape backs out of a shortcut: blur the focused search/filter field and
    //    close any open column-filter panel, so a single-key shortcut (f, n, e…)
    //    works again immediately — e.g. you hit "s" by mistake, press Esc, then "f".
    if (e.key === 'Escape') {
      if (modalOpen()) return;   // a dialog is open -> let it handle its own Escape (close)
      var a = document.activeElement;
      if (a && a.blur && a.matches && a.matches('input, textarea, select')) a.blur();
      var panel = document.querySelector('.sfa-colfilter-panel');
      if (panel) panel.remove();
      return;
    }

    // 1) Filters: Enter walks to the next field (works even though a filter
    //    field IS a form control, which the "isTyping" guard below would skip).
    if (e.key === 'Enter' && isFilterControl(el)) { e.preventDefault(); handleFilterEnter(el); return; }

    // 2) While typing anywhere else, keep every key normal.
    if (isTyping(el)) return;

    var k = (e.key || '').toLowerCase();

    // 3) Alt combos (work even with a modal open — they open the modals).
    if (e.altKey && !e.ctrlKey && !e.metaKey) {
      if (e.shiftKey && k === 's') { e.preventDefault(); focusMenuSearch(); }  // Alt+Shift+S -> top menu search
      else if (k === 'm') { e.preventDefault(); clickByLabel(['add multiple']); }
      else if (k === 'u') { e.preventDefault(); clickByLabel(['bulk upload']); }
      else if (k === 'n') { e.preventDefault(); gridPage('next'); }           // Alt+N -> next table page
      else if (k === 'p') { e.preventDefault(); gridPage('previous'); }       // Alt+P -> previous table page
      return;
    }
    if (e.ctrlKey || e.metaKey) return;

    // 4) Help — '?' is Shift+/
    if (e.key === '?') { e.preventDefault(); toggleHelp(); return; }

    // 5) Plain single-key shortcuts — suppressed while a modal is open so they
    //    never fire behind a dialog the user is filling in.
    if (modalOpen()) return;
    switch (k) {
      case 's': e.preventDefault(); focusGridSearch(); break;      // grid search box
      case 'f': e.preventDefault(); openGridFilter(); break;       // grid column-filter row
      case 'r': e.preventDefault(); clickByLabel(['reset']); break;
      case 'n': e.preventDefault(); clickByLabel(['add single', 'add new']); break;
      case 'e': e.preventDefault(); rowAction('.bi-pencil, .bi-pencil-square'); break;
      case 'd': e.preventDefault(); rowAction('.bi-slash-circle, .bi-trash, .bi-x-circle'); break;
      case 'arrowdown': e.preventDefault(); moveRow(1); break;
      case 'arrowup': e.preventDefault(); moveRow(-1); break;
      default: break;
    }
  }

  // ---- mobile cards: label each body cell with its column header ----------
  /** Give every <td> a data-label (its column's <th> text) so the mobile CSS
   *  can show "Label: value" once the table collapses into per-row cards. */
  function labelCells() {
    document.querySelectorAll('.table-responsive table, table.dataTable').forEach(function (table) {
      var ths = table.querySelectorAll('thead tr:first-child th');
      if (!ths.length) return;
      var labels = Array.prototype.map.call(ths, function (th) { return th.textContent.trim(); });
      table.querySelectorAll('tbody tr').forEach(function (tr) {
        Array.prototype.forEach.call(tr.children, function (td, i) {
          if (labels[i] && !td.hasAttribute('data-label')) td.setAttribute('data-label', labels[i]);
        });
      });
    });
  }

  // ---- back-to-top floating button ----------------------------------------
  // The shell freezes the header/footer and scrolls ONLY the content area
  // (.sfa-content); on small screens the window itself may scroll. So we watch
  // whichever is actually scrolling and scroll that one back up.
  function scrollEl() {
    var c = document.querySelector('.sfa-content');
    return (c && c.scrollHeight > c.clientHeight + 4) ? c : null;
  }
  function scrollPos() { var c = scrollEl(); return c ? c.scrollTop : (window.scrollY || document.documentElement.scrollTop || 0); }

  function initScrollTop() {
    var btn = document.getElementById('sfaScrollTop');
    if (!btn) {                       // lives on <body>, so it survives Shell.rebuild()
      btn = document.createElement('button');
      btn.id = 'sfaScrollTop';
      btn.type = 'button';
      btn.className = 'sfa-scrolltop';
      btn.title = 'Back to top';
      btn.setAttribute('aria-label', 'Back to top');
      btn.innerHTML = '<i class="bi bi-arrow-up"></i>';
      document.body.appendChild(btn);
      btn.addEventListener('click', function () {
        var c = scrollEl();
        (c || window).scrollTo({ top: 0, behavior: 'smooth' });
      });
      window.addEventListener('scroll', function () { toggleScrollTop(btn); }, { passive: true });
    }
    // re-bind the content scroller each init (build() makes a fresh .sfa-content)
    var c = document.querySelector('.sfa-content');
    if (c && !c._sfaScrollBound) {
      c._sfaScrollBound = true;
      c.addEventListener('scroll', function () { toggleScrollTop(btn); }, { passive: true });
    }
    // sit ABOVE a page's own floating action button, if it has one
    btn.classList.toggle('has-fab', !!document.querySelector('.sfa-fab, .fab, [data-fab]'));
    toggleScrollTop(btn);
  }
  function toggleScrollTop(btn) { btn.classList.toggle('show', scrollPos() > 300); }

  // ---- table paging (Alt+N / Alt+P) ---------------------------------------
  function gridPage(dir) {   // dir: 'next' | 'previous'
    var b = document.querySelector('.dataTables_paginate .paginate_button.' + dir + ':not(.disabled)');
    if (b) b.click();
  }

  // ---- global page loader -------------------------------------------------
  // ONE spinner overlay for the whole app; restyle it by editing .sfa-loader in
  // app.css (or replace the markup here). Ref-counted + 150ms debounced so a
  // burst of parallel calls shows a single spinner and a fast call never flashes.
  // Api (js/core/http.js) drives it around every request; call it by hand too:
  //   SFALoader.show();  ... ; SFALoader.hide();
  var ldCount = 0, ldTimer = null;

  // Loader look — fully customisable per tenant from the "Page loader" page
  // (settings/loader.html), which saves to the server; loadLoaderConfig() below
  // fetches that and fills these in. LOADER_IMAGE empty = the CSS ring spinner.
  var LOADER_IMAGE = '';      // image URL (served from the external folder) or ''
  var LOADER_ANIM = 'spin';   // spin | pulse | none  (applies to the image)
  var LOADER_TEXT = '';       // optional message under the loader
  var LOADER_ROUND = false;   // clip the image to a circle
  var LOADER_RING = false;    // spinning arc ring AROUND the image (OptCRM-style)
  var LOADER_BAR = false;     // indeterminate progress bar under the text

  function escHtml(s) {
    return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;')
      .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  }
  // If the configured loader image can't load (missing file / 404), fall back to
  // the ring spinner instead of showing a broken image.
  window.sfaLoaderImgFallback = function (img) {
    var wrap = (img.closest && img.closest('.sfa-loader-ring')) || img;
    var sp = document.createElement('div');
    sp.className = 'sfa-loader-spinner';
    if (wrap.parentNode) wrap.parentNode.replaceChild(sp, wrap);
  };
  function loaderInner() {
    var visual;
    if (LOADER_IMAGE) {
      var roundCls = LOADER_ROUND ? ' round' : '';
      var onerr = ' onerror="sfaLoaderImgFallback(this)"';
      if (LOADER_RING) {
        // the arc ring spins AROUND a static (usually round) logo
        visual = '<div class="sfa-loader-ring"><img class="sfa-loader-img anim-none' + roundCls +
          '" src="' + LOADER_IMAGE + '"' + onerr + ' alt="Loading…" /></div>';
      } else {
        visual = '<img class="sfa-loader-img anim-' + LOADER_ANIM + roundCls + '" src="' + LOADER_IMAGE + '"' + onerr + ' alt="Loading…" />';
      }
    } else {
      visual = '<div class="sfa-loader-spinner"></div>';
    }
    var text = LOADER_TEXT ? '<div class="sfa-loader-text">' + escHtml(LOADER_TEXT) + '</div>' : '';
    var bar = LOADER_BAR ? '<div class="sfa-loader-bar"></div>' : '';
    return '<div class="sfa-loader-box">' + visual + text + bar + '</div>';
  }

  // Per-account localStorage cache so the loader look is NOT re-fetched from the
  // DB on every page — it paints instantly from local and is refreshed from the
  // server only the first time (or after a save on the customiser page).
  function loaderCacheKey() { return 'sfa_loader_u' + (window.Session ? Session.activeIndex() : 0); }
  function applyLoaderConfig(cfg) {
    if (!cfg) return;
    LOADER_ANIM = cfg.anim || 'spin';
    LOADER_TEXT = cfg.text || '';
    LOADER_ROUND = !!cfg.round;
    LOADER_RING = !!cfg.ring;
    LOADER_BAR = !!cfg.bar;
    if (cfg.type === 'image' && cfg.hasImage && cfg.imageName && cfg.companyCode) {
      var ctx = window.Session ? Session.ctx() : '';
      LOADER_IMAGE = ctx + API.loader.publicImage +
        '?companyCode=' + encodeURIComponent(cfg.companyCode) + '&file=' + encodeURIComponent(cfg.imageName);
    } else {
      LOADER_IMAGE = '';
    }
    var el = ensureLoader();
    if (cfg.size) el.style.setProperty('--sfa-loader-size', cfg.size + 'px');
    if (cfg.speed) el.style.setProperty('--sfa-loader-speed', cfg.speed + 'ms');
    el.innerHTML = loaderInner();
  }
  var loaderConfigApplied = false;
  function loadLoaderConfig() {
    if (loaderConfigApplied) return;
    loaderConfigApplied = true;
    // 1) instant paint from local cache — no DB call on normal page navigation
    try {
      var cached = JSON.parse(localStorage.getItem(loaderCacheKey()));
      if (cached) { applyLoaderConfig(cached); return; }
    } catch (e) {}
    // 2) first time only: fetch once, apply, and cache for next time
    if (!window.Api || !window.API || !API.loader) { loaderConfigApplied = false; return; }
    Api.get(API.loader.get, { noLoader: true }).then(function (cfg) {
      if (!cfg) return;
      try { localStorage.setItem(loaderCacheKey(), JSON.stringify(cfg)); } catch (e) {}
      applyLoaderConfig(cfg);
    }).catch(function () { loaderConfigApplied = false; });
  }
  function ensureLoader() {
    var el = document.getElementById('sfaLoader');
    if (!el) {
      el = document.createElement('div');
      el.id = 'sfaLoader';
      el.className = 'sfa-loader';
      el.hidden = true;
      el.innerHTML = loaderInner();
      document.body.appendChild(el);
    }
    return el;
  }
  window.SFALoader = {
    show: function () {
      ldCount++;
      if (!ldTimer) ldTimer = setTimeout(function () { ensureLoader().hidden = false; }, 150);
    },
    hide: function () {
      ldCount = Math.max(0, ldCount - 1);
      if (ldCount === 0) { clearTimeout(ldTimer); ldTimer = null; var el = document.getElementById('sfaLoader'); if (el) el.hidden = true; }
    },
    reset: function () { ldCount = 0; clearTimeout(ldTimer); ldTimer = null; var el = document.getElementById('sfaLoader'); if (el) el.hidden = true; },
    /** Swap the loader visual at runtime. Pass an image URL/data-URI, or '' for the CSS spinner. */
    setImage: function (url) {
      LOADER_IMAGE = url || '';
      var el = document.getElementById('sfaLoader');
      if (el) el.innerHTML = loaderInner();
    },
    /** Drop the local cache, re-fetch the saved loader config, and re-apply it (used by the customiser page after a save). */
    refresh: function () { try { localStorage.removeItem(loaderCacheKey()); } catch (e) {} loaderConfigApplied = false; loadLoaderConfig(); }
  };

  // ---- searchable selects (Select2, loaded on demand) ---------------------
  // Every <select> becomes type-to-search. EXCLUDED: the "Add Multiple" grid
  // rows (.multi-row-table — cloned as native selects, so Select2 there breaks
  // the clone) and the DataTables page-size box. A <select multiple> is handled
  // natively by Select2 (searchable tag box), so multi-selects are fine.
  function debounce(fn, ms) { var t; return function () { var a = arguments, c = this; clearTimeout(t); t = setTimeout(function () { fn.apply(c, a); }, ms); }; }

  var s2loading = false, s2queue = [];
  function addHeadOnce(tag, attr, url) {
    if (document.querySelector(tag + '[' + attr + '="' + url + '"]')) return;
    var el = document.createElement(tag);
    if (tag === 'link') el.rel = 'stylesheet';
    el[attr] = url;
    document.head.appendChild(el);
  }
  function withSelect2(cb) {
    if (window.jQuery && jQuery.fn && jQuery.fn.select2) { cb(); return; }
    s2queue.push(cb);
    if (s2loading) return;
    s2loading = true;
    addHeadOnce('link', 'href', 'https://cdn.jsdelivr.net/npm/select2@4.1.0-rc.0/dist/css/select2.min.css');
    addHeadOnce('link', 'href', 'https://cdn.jsdelivr.net/npm/select2-bootstrap-5-theme@1.3.0/dist/select2-bootstrap-5-theme.min.css');
    var sc = document.createElement('script');
    sc.src = 'https://cdn.jsdelivr.net/npm/select2@4.1.0-rc.0/dist/js/select2.min.js';
    sc.onload = function () { s2loading = false; var q = s2queue.slice(); s2queue.length = 0; q.forEach(function (f) { f(); }); };
    document.head.appendChild(sc);
  }
  function s2Eligible(el) {
    if (!el || el.tagName !== 'SELECT') return false;
    if (window.jQuery(el).hasClass('select2-hidden-accessible')) return false;   // already enhanced
    if (el.closest('.multi-row-table') || el.closest('.dataTables_length')) return false;
    if (el.hasAttribute('data-no-search')) return false;
    if (el.options.length < 2) return false;   // not populated yet — an observer re-runs us once it is
    return true;
  }
  function s2Apply(el) {
    var $ = window.jQuery, $el = $(el);
    $el.select2({
      theme: 'bootstrap-5',
      width: '100%',
      dropdownParent: $el.closest('.modal').length ? $el.closest('.modal') : $(document.body),
      placeholder: $el.attr('data-placeholder') || ($el.find('option').first().text() || 'Select')
    });
    // in a Filters card, picking an option jumps to the next field (keeps the
    // keyboard Enter-flow going through searchable selects too)
    var card = el.closest('.card');
    if (card && card.querySelector('.card-header .bi-funnel')) {
      $el.on('select2:select', function () {
        var controls = filterControls(card), i = controls.indexOf(el);
        if (i > -1 && i < controls.length - 1) setTimeout(function () { focusControl(controls[i + 1]); }, 0);
      });
    }
  }
  function initSelects(scope) {
    if (!window.jQuery) return;
    withSelect2(function () {
      window.jQuery(scope || document).find('select').each(function () {
        if (s2Eligible(this)) s2Apply(this);
      });
    });
  }
  var selectObserver = null;
  function bindSelectObserver() {
    if (selectObserver || !window.MutationObserver) return;
    selectObserver = new MutationObserver(debounce(function (muts) {
      if (!window.jQuery) return;
      var $ = window.jQuery, relevant = false;
      muts.forEach(function (m) {
        var t = m.target;
        if (t && t.tagName === 'SELECT' && !t.closest('.multi-row-table')) {
          relevant = true;
          // a select that was repopulated after enhancement -> rebuild the widget
          if ($(t).hasClass('select2-hidden-accessible')) { try { $(t).select2('destroy'); } catch (e) {} }
        }
        for (var i = 0; m.addedNodes && i < m.addedNodes.length; i++) {
          var n = m.addedNodes[i];
          if (n.nodeType === 1 && (n.tagName === 'SELECT' || (n.querySelector && n.querySelector('select')))) relevant = true;
        }
      });
      if (relevant) initSelects(document);
    }, 150));
    selectObserver.observe(document.body, { childList: true, subtree: true });
  }

  // ---- init (idempotent) --------------------------------------------------
  function init() {
    if (!bound) {
      bound = true;
      document.addEventListener('keydown', onKeyDown, true);
      document.addEventListener('click', onDocClick, true);
      // DataTables rebuilds tbody on every draw — re-label the fresh cells.
      if (window.jQuery) window.jQuery(document).on('draw.dt', labelCells);
      // a modal's selects get searchable only once it is shown (correct dropdownParent)
      if (window.jQuery) window.jQuery(document).on('shown.bs.modal', function (e) { initSelects(e.target); });
      bindSelectObserver();
    }
    var card = filterCard();
    if (card) {
      enhanceFilterCard(card);
      // A report opens with its big Filters card auto-hidden so the data shows
      // first (once per load; the user re-opens it with the header chevron).
      if (!card._sfaAutoCollapsed && document.querySelector('table.dataTable, .table-responsive table')) {
        card._sfaAutoCollapsed = true;
        setCollapsed(card, true);
      }
    }
    labelCells();
    initScrollTop();
    ensureLoader();
    loadLoaderConfig();
    initSelects(document);
  }

  window.UIKit = { init: init, labelCells: labelCells, toggleFilter: toggleFilter };

  // Apply the cached loader look IMMEDIATELY (uikit.js loads before shell.js, and
  // before the shell's boot API calls trigger the loader) — so the CUSTOM loader,
  // not the default ring, shows during the very first blank-page load. First-ever
  // load with no cache still briefly shows the default until the one-time fetch.
  try { loadLoaderConfig(); } catch (e) {}
})(window);
