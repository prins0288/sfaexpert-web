# SFAExpert - working rules for Claude (read this first)

## Links and URLs (important - a missed one breaks the page)
Every in-app page lives under the signed-in account slot: `/u/<N>/...` (e.g. `/u/0/hr/payslip.html`), and under a deploy prefix when the app is mounted on a sub-path.
A link written as `dcr/dcr-submit.html` or `/dashboard.html` loses the slot, the page opens with no sign-in and fails ("Api is not defined", 404 ...).

* **Whenever you add ANY link to an in-app page, build it with the slot** - the same way the menu does:
  * in script: `Session.pageUrl('dcr/dcr-submit.html?view=approval')` (= `Session.uUrl(Session.activeIndex(), rest)`), also for `window.open`, `location.href`, buttons.
  * in HTML / template strings: a plain relative `href="dcr/dcr-submit.html"` is acceptable only because `js/shell.js` rewrites every in-app `.html` link (on load, for links added later, and at click) - but prefer `Session.pageUrl(...)`.
  * never hard-code `/u/0/`, never use an absolute `/page.html`.
  * a link that must NOT be rewritten gets `data-no-slot`.
* After adding a page or a link, open it through `/u/0/...` in the browser test and click it.
* New pages: the usual shell (`<template id="pageContent">`, scripts session.js, endpoints.js, http.js, theme.js, settings.js, i18n.js, app.js, uikit.js, shell.js), API paths only from `js/core/endpoints.js`.

## Language (i18n) - every screen text is translatable
Texts come from `src/main/resources/i18n/lang_en.properties` and `lang_hi.properties` (base languages, loaded by `I18nService`) and a company can override any label in the tenant table `label_master`. The page never hard-codes a language:
* static HTML: put `data-i18n="key"` on the element (`data-i18n-ph` placeholder, `data-i18n-title` tooltip); keep the English text inside as the fallback.
* JS-built HTML: `const T = (k, en) => (window.I18n ? I18n.t(k, en) : en)` and write `T('rep.present', 'Present')`; dates use `I18n.lang` for the locale; re-draw on `I18n.onChange(...)` only when `I18n.lang` really changed (a redraw that calls `I18n.apply` would loop).
* add every new key to BOTH properties files (English and Hindi) - check that no key used in the page is missing; reuse existing keys (`common.*`, `entity.*`) where they exist.
* Pages built before this rule (HR, leave, payroll, geo screens ...) still have English-only text and should be converted when touched.

## Reports - one standard design (every report is built like this)
Reference pages: `report/dcr-report.html` (summary + date-wise report) and `master/client-master.html` (the table look). A new or redesigned report MUST have:
* **Layout:** a `.card` with header (icon + title + period + tools), a compact collapsible filter strip, a one-line KPI strip, then the data card.
* **Enterprise table:** `<table class="table table-striped table-bordered nowrap" data-screen-key="REPORT_KEY">` turned into a grid with `sfaDataTable('#id', opts)` (js/app.js) - this gives Excel export, Column Settings (company-wide column layout), the per-column Filter row, "Show N entries", search, sorting and paging. Do not hand-roll tables or paging.
* **Column Settings needs a registry entry:** every `data-screen-key` grid must be registered in `src/main/java/in/opt/sfa/preference/registry/DefaultColumnRegistry.java` (`register("KEY", col("field", i18nKey|null), ...)`) and each configurable `<th>` needs `data-field="field"` (S.No / Details columns stay without). Without it the dialog says "Unknown screen: KEY".
* **Summary first, details on click:** a summary view (e.g. per employee) and a date-wise view; click a row for the complete details (a modal for an employee's days, the detail page for a single day). Row actions are small outline icon buttons in a `no-export` column. Numbers right-aligned, dates with `data-order` so sorting works, status as small coloured tags.
* **Recent searches (every report):** `js/core/activity.js` - `Activity.record(REPORT_KEY, REPORT_LABEL, filterState(), summaryText())` after every search, a **Recent** button opening a modal that calls `Activity.renderInto('activityList', REPORT_KEY, applyActivity)`, and an `applyActivity(filters)` that restores the filters and searches again.
* **After the report loads the filter card folds away automatically and the page scrolls to the top** so the report is on screen; the **Filters** button shows it again (and scrolls to it). Applies on first load and on every Show.
* **Filters** follow the login level (self / team / all via `TeamScope`), cascading state > HQ > designation > employee, Reset + Show buttons; a server-side "Full Excel" export where the report has more columns than the screen.
* Never show `empId`; employee code or name only. Everything checked at phone and desktop width.

## Database / migrations
* Numbered idempotent SQL in `src/main/resources/sql/NN_name.sql`, one block per tenant DB (`USE acme_db; ...`), guarded with `information_schema` + prepared statements; safe to re-run. Next number: check the highest existing file.
* `ddl-auto: validate`: every entity column needs a migration. New code mostly uses `JdbcTemplate`.
* Menu rows go into `company_menu_self` (insert only when the `page` does not exist; set `is_visible = 1, is_active = 1`).
* Tell the user which migrations to run after every change; the app does not run them.

## Standing decisions from the user
* Reply to the user in Hinglish.
* No BCrypt for login; values are stored as plain text, nothing encrypted.
* No Python in the application itself (Python only for scratch tooling).
* `empId` is never shown on any screen - `empCode` or the name only.
* The DCR has nothing hard-coded: everything comes from master tables (`app_dcr_worktype`, `app_validations` rules ...).
* Salary / HR: nothing statutory or policy related is hard-coded (settings with effective dates); statutory values need a CA check.
* Commit and push to the working branch; no pull request unless asked. Commit trailers as given by the session.

## Testing habits
* Pure calculators have JUnit tests (`src/test/java/.../hr`, `GeoCalcTest`); run `mvn test`.
* UI is checked with Playwright at phone (390px) and desktop widths; test data is cleaned up afterwards.
