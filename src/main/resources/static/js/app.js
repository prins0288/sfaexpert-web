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
    window.sfaDataTable = function (selector, opts) {
        opts = opts || {};
        var dt = $(selector).DataTable($.extend({
            dom: "Bfrtip",
            paging: true,
            pageLength: 25,
            lengthMenu: [[10, 25, 50, -1], [10, 25, 50, "All"]],
            ordering: true,
            responsive: false,
            scrollX: true,
            scrollY: "56vh",
            scrollCollapse: true,
            autoWidth: true,
            buttons: [
                { extend: "excelHtml5", text: '<i class="bi bi-file-earmark-excel"></i> Excel',
                  className: "btn btn-sm btn-accent", exportOptions: { columns: ":not(.no-export)" } }
            ]
        }, opts));
        // keep header/body columns aligned after the shell finishes laying out
        setTimeout(function () { dt.columns.adjust(); }, 60);
        $(window).off("resize.sfadt").on("resize.sfadt", function () { dt.columns.adjust(); });
        return dt;
    };

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

    $(function () { sfaSelect2(document); });

})(jQuery);
