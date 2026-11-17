# Assumptions and defaults

Every item below is a **setting** (or a seed row) that can be changed later without a code change. "Needs your confirmation" items are the ones I would like you to look at before Phase 2.

## Architecture
1. **Company = tenant.** One tenant DB is one company group; `company_id` (default 1, table `hr_company`) is carried on every HR table so more than one legal entity can exist in the same DB. Branches belong to a company and carry the **state** (PT / LWF depend on it).
2. **Employees are not duplicated.** The existing `emp_detail` family stays the master; HR adds `hr_emp_employment`. "Employee code" = existing `emp_code`.
3. **Roles are rows.** HR roles (Super Admin, HR Admin, Payroll Approver, Manager, Employee) live in `hr_role` / `hr_role_permission` / `hr_emp_role`. Until someone is given a role explicitly, the existing hierarchy level gives an implicit role: admin level => HR Admin, super-admin level => Super Admin, anybody with reports => Manager, everybody => Employee. Payroll Approver is never implicit (maker-checker).
4. **Settings are effective-dated key / value rows.** The latest row with `effective_from <= date` and (`effective_to` is null or `>= date`) wins. Saving a new value closes the previous open row the day before. A reason is mandatory. Types: `INT`, `DECIMAL`, `BOOL`, `STRING`, `ENUM`, `JSON`.
5. **Money** is `DECIMAL(12,2)`; rounding (`NEAREST` / `FLOOR` / `CEIL`) is a setting per item. Dates are ISO in the DB, `DD-MMM-YYYY` in the UI, timezone `Asia/Kolkata`.
6. **Audit** is a single table (`hr_audit_log`) written by `AuditLogService` for settings, org masters, roles and (later) leave / attendance / salary / payroll. Rows are never updated or deleted.

## Statutory seed values (supplied in your brief - NOT verified by me)

I seed what the brief says and mark each row `source = 'BRIEF'`. **Please have a CA / labour-law consultant confirm them before production.** They are only defaults in `hr_setting`:

| Setting | Seed | Effective from | Note |
|---|---|---|---|
| `PF_WAGE_CEILING` | 15000 | 01-Apr-2000 | previous ceiling |
| `PF_WAGE_CEILING` | 25000 | 17-Sep-2026 | as in the brief |
| `PF_EMPLOYEE_RATE` / `PF_EMPLOYER_EPF_RATE` / `PF_EMPLOYER_EPS_RATE` | 12 / 3.67 / 8.33 % | 01-Apr-2000 | EPS capped at the ceiling |
| `PF_EDLI_RATE` / `PF_ADMIN_RATE` | 0.5 % / 0.5 % | 01-Apr-2000 | employer cost |
| `ESI_WAGE_LIMIT` / `ESI_WAGE_LIMIT_DISABLED` | 21000 / 25000 | 01-Apr-2000 | |
| `ESI_EMPLOYEE_RATE` / `ESI_EMPLOYER_RATE` | 0.75 / 3.25 % | 01-Apr-2000 | rounding up |
| `PT_ANNUAL_CAP` | 2500 | 01-Apr-2000 | |
| `WAGE_RULE_MIN_PERCENT` | 50 | 21-Nov-2025 | Basic + DA vs total remuneration, mode `WARN` |
| Income-tax slabs, standard deduction, 87A rebate | as in the brief | FY 2026-27 | loaded in Phase 6 |

I could not independently verify the PF ceiling change, the 21-Nov-2025 date, or the FY 2026-27 slabs; I treat the brief as the source.

## Open / my defaults (change anytime)

* Leave-year mode default `financial_year` with FY start April; payroll cycle 1st to month end; pay day = last working day.
* Employee types seeded: Permanent, Probation, Contract, Fixed-term, Intern, Consultant (PF / ESI / leave flags in `hr_employee_type`, editable).
* Probation: 6 months, leave allowed = yes, comp-off = no (settings).
* Maker-checker is **on** (cannot be turned off, only the second approver count is a setting).
* Encryption of PAN / Aadhaar / bank number: planned in Phase 9. **Needs your confirmation:** where the key may live (environment variable vs. key file vs. a KMS). Until then values are only masked in UI / API / logs.
* Payslip PDF library (Phase 8): I plan to use OpenPDF (small, LGPL) unless you prefer another one.
* Biometric / device import (Phase 2) starts as CSV import; a vendor API needs the device model.
* Leave / comp-off approvals reuse the existing `ApprovalEngine` flows (auto / manager / admin / manager -> admin) plus delegation and auto-approve days.
