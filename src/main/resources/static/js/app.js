/* StarSFA shared front-end helpers */
(function ($) {
    "use strict";

    // NOTE: the sidebar / drawer toggle is owned by shell.js (#sfaToggle). The
    // old jQuery handler that also lived here was removed — having both toggled
    // `sidebar-open` twice per click, so the mobile drawer never opened.

    // Initialise a standard master listing DataTable.
    // scrollY + scrollCollapse give a FROZEN header whose column widths stay
    // exactly aligned with the body while the rows scroll (DataTables computes
    // and locks the column sizes); scrollX keeps wide tables horizontally
    // scrollable. columns.adjust() re-syncs widths after layout/resize.
    //
    // Every table, everywhere, also gets: a Columns (show/hide) button, an
    // Excel-style Filter button (a second header row, one multi-select
    // dropdown per column — see sfaInsertFilterRow/sfaWireColumnFilters
    // below), and click-a-row-to-highlight-it (only one row highlighted at
    // a time).
    window.sfaDataTable = function (selector, opts) {
        opts = opts || {};
        var $table = $(selector);
        // Content Protection hides ONLY the Excel export button when on — the
        // Columns/Filter buttons aren't a "download", they stay either way.
        // Uses the server-VERIFIED flag (sfaApplyContentProtection), NOT
        // Settings.contentProtectionEnabled()'s cached value — that cache is
        // editable from the DevTools console, this isn't.
        var protectedMode = window.sfaContentProtectionActive && sfaContentProtectionActive();
        // Column Settings: a <table data-screen-key="KEY"> gets the company's saved
        // column order/visibility (see sfaApplyColumnLayout below) applied to
        // the DOM BEFORE DataTables reads it — so every index-based feature
        // (filter row, export, mobile labels) just sees the final layout.
        var screenKey = $table.attr("data-screen-key") || null;
        var layout = screenKey ? sfaApplyColumnLayout($table.get(0), SFA_COLPREFS[screenKey]) : null;
        var buttons = [];
        if (!protectedMode) {
            buttons.push({ extend: "excelHtml5", text: '<i class="bi bi-file-earmark-excel"></i> Excel',
                className: "btn btn-sm btn-accent",
                // with Column Settings, export what the user chose to see
                exportOptions: { columns: screenKey ? ":visible:not(.no-export)" : ":not(.no-export)" } });
        }
        // NOTE: plain "btn-outline-secondary" clashes with the "btn-secondary"
        // DataTables' Bootstrap5 integration also adds to every button — same
        // gray on both text AND background, so the label goes invisible. Use
        // our own explicitly-styled class instead (see app.css .sfa-btn-outline).
        if (screenKey && sfaCanEditColumns()) {
            // saved for the whole company (COLUMN_SETTINGS permission); replaces the session-only colvis toggle
            buttons.push({ text: '<i class="bi bi-layout-three-columns"></i> <span data-i18n="columns.settings">Column Settings</span>',
                className: "btn btn-sm sfa-btn-outline",
                action: function () { sfaOpenColumnSettings($table.get(0)); } });
        } else {
            buttons.push({ extend: "colvis", text: '<i class="bi bi-eye-slash"></i> Columns', className: "btn btn-sm sfa-btn-outline" });
        }
        buttons.push({
            text: '<i class="bi bi-funnel"></i> Filter',
            className: "btn btn-sm sfa-btn-outline",
            // scrollX/scrollY (below) clone the header into a separate
            // .dataTables_scrollHead wrapper for display — that CLONE, not
            // the original table's now sizing-only <thead>, is what's
            // actually visible, so both copies of the row must be kept in
            // sync explicitly (a plain .toggleClass() on the pair would
            // flip them to opposite states instead of together).
            action: function () {
                var $rows = $table.closest(".dataTables_wrapper").find("thead tr.sfa-filter-row");
                var hidden = $rows.first().hasClass("d-none");
                $rows.toggleClass("d-none", !hidden);
                dt.columns.adjust();
            }
        });

        // Must exist in <thead> BEFORE .DataTable() runs — scrollX/scrollY
        // clone the header into a separate fixed wrapper at init time, so a
        // row added afterward would never show up in the visible header.
        sfaInsertFilterRow($table);

        var dt = $table.DataTable($.extend({
            dom: "Blfrtip",   // "l" = the page-size dropdown (10/20/25/50/100/All) — was missing entirely before
            paging: true,
            pageLength: 10,
            lengthMenu: [[10, 20, 25, 50, 100, -1], [10, 20, 25, 50, 100, "All"]],
            ordering: true,
            orderCellsTop: true, // sort clicks bind to the real header row, not the filter row below it
            responsive: false,
            scrollX: true,
            scrollY: "56vh",
            scrollCollapse: true,
            autoWidth: true,
            buttons: buttons
        }, opts, layout ? { columnDefs: (opts.columnDefs || []).concat(layout.columnDefs) } : {}));
        $table.get(0)._sfaOpts = opts;   // sfaRebuildTable() re-inits with the same options
        $table.get(0)._sfaDt = dt;

        sfaWireColumnFilters($table, dt);

        // Click a row to highlight it; clicking a different row moves the
        // highlight (only one at a time) — action buttons inside the row
        // (edit/status) still fire normally, this only adds the highlight.
        $table.off("click.sfarowsel", "tbody tr").on("click.sfarowsel", "tbody tr", function () {
            $table.find("tbody tr.sfa-row-selected").removeClass("sfa-row-selected");
            $(this).addClass("sfa-row-selected");
        });

        // keep header/body columns aligned after the shell finishes laying out
        setTimeout(function () { dt.columns.adjust(); }, 60);
        $(window).off("resize.sfadt").on("resize.sfadt", function () { dt.columns.adjust(); });
        // DataTables just created its toolbar buttons (Excel/Columns/Filter) and
        // cloned the header — translate that freshly-injected chrome. Data cells
        // in <tbody> are untouched (i18n's auto pass never looks inside tbody).
        if (window.I18n) setTimeout(function () { I18n.apply(); }, 0);
        return dt;
    };

    // ---- Column Settings: company-wide column order / visibility -----------
    // Generic for every grid. A page opts in with markup only:
    //     <table data-screen-key="DCR_REPORT"> ... <th data-field="date">Date</th>
    // and the screen key is registered (with its default columns) in the
    // backend's DefaultColumnRegistry. Layouts come from GET /api/preferences/{key}
    // (saved layout merged with defaults, or the defaults) and are fetched
    // once per tab before the page renders (sfaColumnPrefsPreload, called by
    // shell.js). <th>s without data-field (S.No, Actions) are not
    // configurable and keep their position. Any failure -> the page's own
    // column order, all visible: nothing breaks.
    // The layout is per COMPANY (same for every user); only users granted the
    // COLUMN_SETTINGS permission get the Column Settings button — others keep
    // the session-only Columns toggle and simply see the company layout.
    var SFA_COLPREFS = {};   // screenKey -> { screenKey, customized, columns: [...] } | null
    var SORTABLE_SRC = "https://cdn.jsdelivr.net/npm/sortablejs@1.15.2/Sortable.min.js";

    // Needs the COLUMN_SETTINGS permission, which is DENIED by default — an
    // admin grants it per emp_id / designation / emp_level (Permission Master).
    // The server enforces the same code on save/reset.
    function sfaCanEditColumns() {
        return !!(window.SFA && SFA.granted && SFA.granted("COLUMN_SETTINGS"));
    }

    function sfaLoadColumnPrefs(key) {
        var cached = Session.cache.get("colprefs_" + key);
        if (cached) { SFA_COLPREFS[key] = cached; return Promise.resolve(cached); }
        return Api.get(API.preferences.screen(key), { noLoader: true }).then(function (res) {
            SFA_COLPREFS[key] = res;
            Session.cache.set("colprefs_" + key, res);
            return res;
        }).catch(function () { SFA_COLPREFS[key] = null; return null; });
    }

    function sfaStoreColumnPrefs(key, res) {
        SFA_COLPREFS[key] = res;
        Session.cache.set("colprefs_" + key, res);
    }

    /** Fetch layouts for every data-screen-key grid in the page template (before it is rendered). */
    window.sfaColumnPrefsPreload = function () {
        var tpl = document.getElementById("pageContent");
        var root = (tpl && tpl.content) ? tpl.content : document;
        var keys = {};
        root.querySelectorAll("table[data-screen-key]").forEach(function (t) { keys[t.getAttribute("data-screen-key")] = true; });
        return Promise.all(Object.keys(keys).map(sfaLoadColumnPrefs));
    };

    /** Stable key per header cell: its data-field, or a fixed slot for non-configurable columns. */
    function sfaHeaderKeys(table) {
        var cells = Array.prototype.slice.call(table.tHead.rows[0].cells);
        if (!table._sfaOrigKeys) {
            table._sfaOrigKeys = cells.map(function (th, i) {
                var k = th.getAttribute("data-field") || ("__fixed" + i);
                th.setAttribute("data-sfa-key", k);
                return k;
            });
        }
        return cells.map(function (th) { return th.getAttribute("data-sfa-key"); });
    }

    /**
     * Physically reorder the header and body cells to the saved layout and
     * return the DataTables columnDefs for hidden columns / widths. Called on
     * every init: pages destroy() + re-render <tbody> on each refresh, so a
     * freshly rendered row is in source order while the header (and any row
     * kept across a rebuild) is in the last applied order — each row is
     * stamped with the order it is in, so both cases are handled.
     */
    function sfaApplyColumnLayout(table, prefs) {
        if (!table.tHead || !table.tHead.rows.length) return null;
        var current = sfaHeaderKeys(table);
        var orig = table._sfaOrigKeys;
        if (current.length !== orig.length) return null;   // header changed under us: leave it alone

        var fields = orig.filter(function (k) { return k.indexOf("__fixed") !== 0; });
        var byField = {};
        var ordered = [];
        ((prefs && prefs.columns) || []).forEach(function (c) {
            if (c && fields.indexOf(c.field) !== -1 && !byField[c.field]) { byField[c.field] = c; ordered.push(c.field); }
        });
        fields.forEach(function (f) { if (ordered.indexOf(f) === -1) ordered.push(f); });   // not in the registry: keep, visible
        var next = 0;
        var target = orig.map(function (k) { return k.indexOf("__fixed") === 0 ? k : ordered[next++]; });

        var headRow = table.tHead.rows[0];
        var thByKey = {};
        Array.prototype.forEach.call(headRow.cells, function (th) { thByKey[th.getAttribute("data-sfa-key")] = th; });
        target.forEach(function (k) { headRow.appendChild(thByKey[k]); });

        var targetSig = target.join("|");
        Array.prototype.forEach.call(table.tBodies, function (tb) {
            Array.prototype.forEach.call(tb.rows, function (tr) {
                var from = tr.getAttribute("data-sfa-order");
                if (from === targetSig) return;
                var fromKeys = from ? from.split("|") : orig;
                if (tr.cells.length !== fromKeys.length) return;    // e.g. a colspan "no data" row
                var cellByKey = {};
                Array.prototype.forEach.call(tr.cells, function (td, i) { cellByKey[fromKeys[i]] = td; });
                target.forEach(function (k) { tr.appendChild(cellByKey[k]); });
                tr.setAttribute("data-sfa-order", targetSig);
            });
        });

        var defs = [], hidden = [];
        target.forEach(function (k, i) {
            var c = byField[k];
            if (!c) return;
            if (c.visible === false) hidden.push(i);
            if (c.width) defs.push({ targets: [i], width: c.width });
        });
        if (hidden.length) defs.push({ targets: hidden, visible: false });
        return { columnDefs: defs };
    }

    /** destroy() + re-init with the current layout, keeping the page's own DT handle valid. */
    function sfaRebuildTable(table) {
        var old = table._sfaDt;
        if (!old) return;
        old.destroy();   // DataTables re-shows hidden columns, so every cell is back in the DOM
        var fresh = window.sfaDataTable(table, table._sfaOpts || {});
        // Pages keep `DT = sfaDataTable(...)` and later call DT.destroy(); point that
        // (now stale) API object at the new instance so it keeps working.
        old.context.length = 0;
        Array.prototype.push.apply(old.context, fresh.context);
        table._sfaDt = old;
    }

    function sfaLoadScript(src) {
        return new Promise(function (resolve, reject) {
            if (document.querySelector('script[src="' + src + '"]')) { resolve(); return; }
            var el = document.createElement("script");
            el.src = src; el.onload = resolve; el.onerror = reject;
            document.head.appendChild(el);
        });
    }

    function sfaColumnModal() {
        var el = document.getElementById("sfaColModal");
        if (el) return el;
        el = document.createElement("div");
        el.className = "modal fade";
        el.id = "sfaColModal";
        el.tabIndex = -1;
        el.innerHTML =
            '<div class="modal-dialog modal-dialog-scrollable modal-fullscreen-sm-down"><div class="modal-content">' +
            '<div class="modal-header sfa-modal"><h5 class="modal-title"><i class="bi bi-layout-three-columns"></i> ' +
            '<span data-i18n="columns.settings">Column Settings</span></h5>' +
            '<button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>' +
            '<div class="modal-body">' +
            '<p class="small text-muted mb-2" data-i18n="columns.help">Drag to reorder, untick to hide. Applies to every user of your company on this screen.</p>' +
            '<div class="alert alert-danger py-1 small d-none" id="sfaColError"></div>' +
            '<ul class="list-group sfa-col-list" id="sfaColList"></ul></div>' +
            '<div class="modal-footer">' +
            '<button type="button" class="btn btn-outline-secondary me-auto" id="sfaColReset"><i class="bi bi-arrow-counterclockwise"></i> ' +
            '<span data-i18n="columns.resetDefault">Reset to Default</span></button>' +
            '<button type="button" class="btn btn-light" data-bs-dismiss="modal" data-i18n="common.cancel">Cancel</button>' +
            '<button type="button" class="btn btn-primary" id="sfaColApply" data-i18n="columns.apply">Apply</button>' +
            '</div></div></div>';
        document.body.appendChild(el);
        // up/down buttons: keyboard-friendly, and still work if SortableJS can't load
        $(el).on("click", ".sfa-col-move", function () {
            var li = this.closest("li");
            if (this.getAttribute("data-dir") === "up" && li.previousElementSibling) li.parentNode.insertBefore(li, li.previousElementSibling);
            if (this.getAttribute("data-dir") === "down" && li.nextElementSibling) li.parentNode.insertBefore(li.nextElementSibling, li);
        });
        if (window.I18n) I18n.apply(el);
        return el;
    }

    function sfaColError(msg) {
        var e = document.getElementById("sfaColError");
        e.textContent = msg || "";
        e.classList.toggle("d-none", !msg);
    }

    /** Open the Column Settings dialog for one grid. */
    window.sfaOpenColumnSettings = function (table) {
        var key = table.getAttribute("data-screen-key");
        var prefs = SFA_COLPREFS[key];
        var modalEl = sfaColumnModal();
        var list = modalEl.querySelector("#sfaColList");
        sfaColError("");

        // current header labels by field (already translated on the page). Read
        // through DataTables: a hidden column's <th> is detached from the DOM.
        var labels = {};
        var headers = table._sfaDt ? table._sfaDt.columns().header().toArray() : table.tHead.rows[0].cells;
        Array.prototype.forEach.call(headers, function (th) {
            var f = th.getAttribute("data-field");
            if (f) labels[f] = th.textContent.trim();
        });
        var cols = [];
        ((prefs && prefs.columns) || []).forEach(function (c) { if (labels[c.field] !== undefined) cols.push(c); });
        Object.keys(labels).forEach(function (f) {
            if (!cols.some(function (c) { return c.field === f; })) cols.push({ field: f, visible: true });
        });

        list.innerHTML = cols.map(function (c) {
            var label = (c.labelKey && window.I18n) ? I18n.t(c.labelKey, labels[c.field]) : labels[c.field];
            return '<li class="list-group-item d-flex align-items-center gap-2" data-field="' + SFA.esc(c.field) + '">' +
                '<i class="bi bi-grip-vertical text-muted sfa-col-handle" style="cursor:grab"></i>' +
                '<input class="form-check-input mt-0" type="checkbox" ' + (c.visible === false ? "" : "checked") + '>' +
                '<span class="flex-grow-1">' + SFA.esc(label) + '</span>' +
                '<button type="button" class="btn btn-sm btn-link p-0 sfa-col-move" data-dir="up" title="Up"><i class="bi bi-chevron-up"></i></button>' +
                '<button type="button" class="btn btn-sm btn-link p-0 sfa-col-move" data-dir="down" title="Down"><i class="bi bi-chevron-down"></i></button>' +
                '</li>';
        }).join("");

        sfaLoadScript(SORTABLE_SRC).then(function () {
            if (window.Sortable && !list._sfaSortable) {
                list._sfaSortable = Sortable.create(list, { handle: ".sfa-col-handle", animation: 150 });
            }
        }).catch(function () { /* up/down buttons still work */ });

        function done(res) {
            sfaStoreColumnPrefs(key, res);
            bootstrap.Modal.getOrCreateInstance(modalEl).hide();
            document.querySelectorAll('table[data-screen-key="' + key + '"]').forEach(sfaRebuildTable);
        }

        modalEl.querySelector("#sfaColApply").onclick = function () {
            var body = Array.prototype.map.call(list.children, function (li, i) {
                return { field: li.getAttribute("data-field"), order: i, visible: li.querySelector("input").checked };
            });
            if (!body.some(function (c) { return c.visible; })) {
                sfaColError(window.I18n ? I18n.t("columns.atLeastOne", "Keep at least one column visible.") : "Keep at least one column visible.");
                return;
            }
            Api.post(API.preferences.screen(key), body).then(done).catch(function (e) { sfaColError(e.message); });
        };
        modalEl.querySelector("#sfaColReset").onclick = function () {
            Api.del(API.preferences.screen(key)).then(done).catch(function (e) { sfaColError(e.message); });
        };

        bootstrap.Modal.getOrCreateInstance(modalEl).show();
    };

    // ---- Excel-style per-column filter row -------------------------------
    // A second <thead> row (hidden until the "Filter" toolbar button is
    // clicked) with one dropdown per column; each dropdown lists that
    // column's distinct values as checkboxes and narrows the table to rows
    // matching every column's active selection — same idea as Excel's
    // AutoFilter. The S.No column and any ".no-export" column (Actions)
    // are skipped since they're not meaningful to filter on.
    function sfaFilterableColumns($table) {
        var cols = [];
        $table.find("thead tr").first().find("th").each(function (i) {
            var label = $(this).text().trim();
            var skip = $(this).hasClass("no-export") || !label || label.toLowerCase() === "s.no";
            cols.push({ index: i, label: label, skip: skip });
        });
        return cols;
    }

    function sfaInsertFilterRow($table) {
        // reload() destroy()s + rebuilds the DataTable on every refresh — drop
        // any row from a previous init first, or they'd stack on top of each other.
        $table.find("thead tr.sfa-filter-row").remove();
        var $tr = $('<tr class="sfa-filter-row d-none"></tr>');
        sfaFilterableColumns($table).forEach(function (c) {
            if (c.skip) { $tr.append("<th></th>"); return; }
            $tr.append(
                '<th><button type="button" class="sfa-colfilter-btn" data-col="' + c.index + '">' +
                '<span class="sfa-colfilter-label">' + SFA.esc(c.label) + '</span><i class="bi bi-caret-down-fill"></i></button></th>'
            );
        });
        $table.find("thead").append($tr);
    }

    function sfaWireColumnFilters($table, dt) {
        var tableNode = $table.get(0);
        var active = {}; // { colIndex: Set(selectedValues) }

        // $.fn.dataTable.ext.search is a GLOBAL array shared by every table on
        // the page — tag our predicate with the table node and drop any
        // earlier one for this same table first, or each reload() would stack
        // another (harmless but ever-growing) predicate on top of the last.
        $.fn.dataTable.ext.search = $.fn.dataTable.ext.search.filter(function (fn) {
            return fn._sfaFilterTable !== tableNode;
        });
        var predicate = function (settings, searchData, dataIndex) {
            if (settings.nTable !== tableNode) return true;
            for (var col in active) {
                if (!active[col] || !active[col].size) continue;
                var node = dt.cell(dataIndex, col).node();
                var val = node ? $(node).text().trim() : "";
                if (!active[col].has(val)) return false;
            }
            return true;
        };
        predicate._sfaFilterTable = tableNode;
        $.fn.dataTable.ext.search.push(predicate);

        function closePanel() { $(".sfa-colfilter-panel").remove(); $(document).off("mousedown.sfacf"); }

        // scrollX/scrollY clone the header into .dataTables_scrollHead for
        // display (see the Filter button's action above) — that clone lives
        // OUTSIDE $table's own DOM subtree, so delegation must be bound to
        // the shared wrapper, not to $table itself, or clicks on the
        // (only) visible copy of the buttons would never be seen.
        var $scope = $table.closest(".dataTables_wrapper");
        $scope.off("click.sfacf", ".sfa-colfilter-btn").on("click.sfacf", ".sfa-colfilter-btn", function (e) {
            e.stopPropagation();
            var $btn = $(this);
            var col = parseInt($btn.data("col"), 10);
            if ($(".sfa-colfilter-panel[data-col='" + col + "']").length) { closePanel(); return; }
            closePanel();

            var baseLabel = $btn.data("baseLabel");
            if (baseLabel === undefined) { baseLabel = $btn.find(".sfa-colfilter-label").text(); $btn.data("baseLabel", baseLabel); }

            var values = {};
            dt.column(col, { search: "none" }).nodes().each(function (td) {
                var v = $(td).text().trim();
                if (v !== "") values[v] = true;
            });
            var sorted = Object.keys(values).sort(function (a, b) { return a.localeCompare(b, undefined, { numeric: true }); });
            var selected = active[col] || new Set();

            var $panel = $('<div class="sfa-colfilter-panel" data-col="' + col + '"></div>');
            $panel.append('<input type="text" class="form-control form-control-sm sfa-colfilter-search" placeholder="Search...">');
            $panel.append('<div class="sfa-colfilter-actions"><a href="#" data-act="all">Select all</a><a href="#" data-act="none">Clear</a></div>');
            var $list = $('<div class="sfa-colfilter-list"></div>');
            sorted.forEach(function (v) {
                var checked = selected.has(v) ? "checked" : "";
                $list.append('<label class="sfa-colfilter-item"><input type="checkbox" value="' + SFA.esc(v) + '" ' + checked + '> ' + SFA.esc(v) + '</label>');
            });
            $panel.append($list);
            $("body").append($panel);

            var rect = $btn.get(0).getBoundingClientRect();
            $panel.css({ top: (rect.bottom + 4) + "px", left: rect.left + "px" });
            var pw = $panel.outerWidth();
            if (rect.left + pw > window.innerWidth - 8) $panel.css("left", Math.max(8, window.innerWidth - pw - 8) + "px");

            function apply() {
                var chosen = new Set();
                $list.find("input:checked").each(function () { chosen.add(this.value); });
                if (chosen.size) active[col] = chosen; else delete active[col];
                $btn.toggleClass("sfa-colfilter-active", !!active[col]);
                $btn.find(".sfa-colfilter-label").text(active[col] ? (baseLabel + " (" + chosen.size + ")") : baseLabel);
                dt.draw();
            }

            $list.on("change", "input", apply);
            $panel.on("click", "[data-act]", function (e) {
                e.preventDefault();
                var act = $(this).data("act");
                $list.find("input").prop("checked", act === "all");
                apply();
            });
            $panel.on("input", ".sfa-colfilter-search", function () {
                var q = this.value.trim().toLowerCase();
                $list.find(".sfa-colfilter-item").each(function () {
                    $(this).toggle($(this).text().toLowerCase().indexOf(q) !== -1);
                });
            });
            $panel.on("mousedown", function (e) { e.stopPropagation(); });
            setTimeout(function () { $(document).on("mousedown.sfacf", closePanel); }, 0);
        });
    }

    // Initialise Select2 on any .sfa-select within a scope (default document/modal).
    window.sfaSelect2 = function (scope) {
        $(scope || document).find(".sfa-select").each(function () {
            var $el = $(this);
            $el.select2({
                theme: "bootstrap-5",
                width: "100%",
                dropdownParent: $el.closest(".modal").length ? $el.closest(".modal") : $(document.body),
                placeholder: $el.data("placeholder") || "Select"
            });
        });
    };

    // Initialise Flatpickr on any .sfa-date input within a scope (default document/modal).
    // altInput:true keeps the ORIGINAL input's .value as ISO "Y-m-d" (so existing
    // save JS that READS .value to send to the API keeps working unchanged) while
    // the user sees/types the company's configured format (js/core/settings.js)
    // in a separate visible text input Flatpickr creates alongside it.
    // IMPORTANT: populating a value from JS (edit forms) must go through
    // sfaSetDate() below, NOT `el.value = ...` directly — Flatpickr's altInput
    // doesn't observe raw DOM value assignment, so the visible box would silently
    // stay blank/stale even though the hidden field (and thus a save) is correct.
    window.sfaFlatpickr = function (scope) {
        if (!window.flatpickr) return;
        var fmt = (window.Settings && Settings.flatpickrFormat()) || "d-m-Y";
        $(scope || document).find(".sfa-date").each(function () {
            if (this._flatpickr) { this._flatpickr.set("altFormat", fmt); return; }
            flatpickr(this, {
                altInput: true,
                altFormat: fmt,
                altInputClass: "form-control",
                dateFormat: "Y-m-d",
                allowInput: true
            });
        });
    };

    // Set a .sfa-date field's value (ISO "yyyy-MM-dd", or "" / null to clear) from JS —
    // e.g. populating an Edit modal. Goes through the Flatpickr instance so the
    // VISIBLE (altInput) box updates too, not just the hidden underlying input.
    window.sfaSetDate = function (id, isoValue) {
        var el = document.getElementById(id);
        if (!el) return;
        if (el._flatpickr) { el._flatpickr.setDate(isoValue || null, false); }
        else { el.value = isoValue || ""; }
    };

    // Multiple-row entry: clone the last row of a .multi-row-table tbody.
    window.sfaAddRow = function (tableId) {
        var $tbody = $("#" + tableId + " tbody");
        var $last = $tbody.find("tr:last");
        var $clone = $last.clone();
        $clone.find("input, select, textarea").val("");
        $clone.find(".sfa-select").each(function () {
            if ($(this).data("select2")) { $(this).select2("destroy"); }
            $(this).removeClass("select2-hidden-accessible").next(".select2").remove();
        });
        $tbody.append($clone);
        sfaSelect2($clone);
        sfaRenumber(tableId);
    };

    window.sfaRemoveRow = function (btn, tableId) {
        var $tbody = $("#" + tableId + " tbody");
        if ($tbody.find("tr").length > 1) {
            $(btn).closest("tr").remove();
            sfaRenumber(tableId);
        } else {
            $(btn).closest("tr").find("input, select, textarea").val("");
        }
    };

    window.sfaRenumber = function (tableId) {
        $("#" + tableId + " tbody tr").each(function (i) {
            $(this).find(".row-sno").text(i + 1);
        });
    };

    /**
     * Content Protection — ONE company-wide toggle (Settings > contentProtection,
     * js/core/settings.js) that turns on together: hiding every table's Excel
     * export button (see sfaDataTable above), blocking right-click / copy / cut /
     * text-selection / common save-page shortcuts, and a DevTools-open deterrent.
     *
     * IMPORTANT — read before relying on this for anything sensitive:
     * this is a DETERRENT for casual users, not real security. No web page can
     * actually prevent a browser's built-in DevTools from opening — browsers
     * deliberately don't expose that control to page JavaScript. A technical
     * user can always bypass every part of this (disable JavaScript, view-source,
     * an un-docked DevTools window our size-heuristic below won't catch, the
     * network tab, browser reader mode, a proxy, redefining these very functions
     * from the console, etc.). If data must actually be kept from a user, that's
     * a server-side authorization decision, not a client-side one — this only
     * stops accidental/casual copying by someone who isn't trying to bypass it.
     *
     * Deliberately does NOT trust js/core/settings.js's cached value — that
     * cache lives in sessionStorage, which is trivially editable from the
     * DevTools console (`sessionStorage.setItem(...)`), which would otherwise
     * let anyone silently flip this off. Verifies fresh against the server
     * every page load instead. (This closes THAT specific bypass only — see
     * the paragraph above for the ones that can never be closed client-side.)
     */
    var _contentProtectionActive = false;
    window.sfaContentProtectionActive = function () { return _contentProtectionActive; };

    window.sfaApplyContentProtection = async function () {
        if (!window.API || !window.Api) return;
        let verified;
        try { verified = await Api.get(API.settings.get); } catch (e) { return; }
        if (!verified || verified.contentProtection !== "Y") return;
        _contentProtectionActive = true;
        if (document.documentElement.classList.contains("sfa-protected")) return; // already wired
        document.documentElement.classList.add("sfa-protected");

        var inFormField = function (el) {
            return !!(el && el.closest && el.closest('input, textarea, select, [contenteditable="true"]'));
        };

        ["contextmenu", "copy", "cut", "selectstart", "dragstart"].forEach(function (evt) {
            document.addEventListener(evt, function (e) {
                if (inFormField(e.target)) return;
                e.preventDefault();
            }, true);
        });

        document.addEventListener("keydown", function (e) {
            if (inFormField(e.target)) return;
            var k = (e.key || "").toLowerCase();
            var mod = e.ctrlKey || e.metaKey;
            if (k === "f12") { e.preventDefault(); return; }
            if (mod && e.shiftKey && (k === "i" || k === "j" || k === "c")) { e.preventDefault(); return; } // common devtools shortcuts
            if (mod && (k === "c" || k === "x" || k === "s" || k === "p")) e.preventDefault(); // copy/cut/save/print
        }, true);

        // Best-effort DevTools-open DETERRENT only — see the big comment above.
        // Heuristic: a docked DevTools panel usually leaves a large gap between
        // outer (browser chrome) and inner (page viewport) dimensions.
        var warned = false;
        setInterval(function () {
            var open = (window.outerWidth - window.innerWidth) > 160 || (window.outerHeight - window.innerHeight) > 160;
            if (open && !warned) { warned = true; sfaShowProtectionWarning(); }
            else if (!open) { warned = false; }
        }, 1000);
    };

    window.sfaShowProtectionWarning = function () {
        if (document.getElementById("sfaProtectionWarning")) return;
        var el = document.createElement("div");
        el.id = "sfaProtectionWarning";
        el.className = "sfa-protection-warning";
        el.innerHTML = '<div><i class="bi bi-shield-exclamation"></i> This screen is protected. Please close developer tools to continue.</div>';
        document.body.appendChild(el);
        setTimeout(function () { var e = document.getElementById("sfaProtectionWarning"); if (e) e.remove(); }, 4000);
    };

    $(function () { sfaSelect2(document); sfaFlatpickr(document); });

})(jQuery);
