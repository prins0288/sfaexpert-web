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
        var buttons = [];
        if (!protectedMode) {
            buttons.push({ extend: "excelHtml5", text: '<i class="bi bi-file-earmark-excel"></i> Excel',
                className: "btn btn-sm btn-accent", exportOptions: { columns: ":not(.no-export)" } });
        }
        // NOTE: plain "btn-outline-secondary" clashes with the "btn-secondary"
        // DataTables' Bootstrap5 integration also adds to every button — same
        // gray on both text AND background, so the label goes invisible. Use
        // our own explicitly-styled class instead (see app.css .sfa-btn-outline).
        buttons.push({ extend: "colvis", text: '<i class="bi bi-eye-slash"></i> Columns', className: "btn btn-sm sfa-btn-outline" });
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
        }, opts));

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
