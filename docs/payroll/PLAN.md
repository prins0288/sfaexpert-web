# HR + Leave + Payroll (India) - Plan

Status: **plan for review** (Phase 1 is built alongside this document). Nothing statutory or policy related is hard-coded:
every rate, ceiling, limit, slab and rule is a row in a settings table with `effective_from` / `effective_to`.

## 1. What the repo already is (findings)

| Area | What exists | How the HR module uses it |
|---|---|---|
| Stack | Spring Boot 4.1 / Java 21 (WAR), JdbcTemplate + JPA, MySQL 8, plain-JS pages (Bootstrap 5, no build step), Apache POI for Excel | Same stack. No new framework. Calculations are pure Java in `in.opt.sfa.tenant.hr.calc` (JUnit 5 added - there was no test setup). |
| Tenancy | One DB per tenant (`TenantContext`, routed `JdbcTemplate`, central DB `sfa_central`) | A tenant DB = one company (group). `company_id` is still on every HR table (default 1) so several legal entities can live in one DB. |
| Employees | `emp_detail` (+ `emp_statutory`, `emp_bank_account`, `emp_personal`, `emp_address`, `emp_nominee`, `emp_prev_employment`, `emp_assignment_history`), designation / state / HQ / division masters, manager chain (`manager_id`), promotion history | Employee master is **extended, not replaced**: new `hr_emp_employment` (branch, department, grade, cost centre, employee type, work type, 2nd-level manager, probation, notice) and extra statutory columns. |
| Roles | Role = designation hierarchy level (User / Manager / Admin / Super Admin, `LevelRoleService`), `permission_master` + `permission_assignment` (emp > designation > level), `TeamScope` (self / team / all) | HR roles (Super Admin, HR Admin, Payroll Approver, Manager, Employee) become **DB rows** (`hr_role`, `hr_role_permission`, `hr_emp_role`). Implicit roles derive from the existing level (admin level => HR Admin rights until explicitly assigned, manager => Manager, everyone => Employee). Team visibility still uses `TeamScope`. |
| Calendar | `holiday_master` + `holiday_assignment` (state / HQ / employee), `weekly_off_rule` (nth weekday), company weekly-off setting, DCR (attendance proof), tour plan | Phase 2 **extends** these into Work Types and `classifyDay()`; DCR submission is one attendance source. |
| Approval | `ApprovalEngine` (auto / manager / admin / manager->admin), history tables | Leave / comp-off / regularisation approvals reuse it (+ delegation, auto-approve). |
| Audit | `menu_audit_log` only | New generic `hr_audit_log` (who / when / entity / field / old / new / reason). |
| Settings | `company_setting_master` (key/value, central DB), `app_setting` | New effective-dated `hr_setting` + `hr_setting_def` (catalogue with type, group, default, validation). |
| Migrations | Numbered `NN_name.sql`, run by hand for every tenant DB, idempotent | Same: `101_hr_foundation.sql` ... one file per phase, each repeatable. |

## 2. Design rules

1. **Money**: `DECIMAL(12,2)` (or integer paise inside calculators). Never float/double. Rounding mode per component / statutory item is a setting (`NEAREST`, `FLOOR`, `CEIL`).
2. **Effective dating**: every setting row has `effective_from` (required) and `effective_to` (nullable). `EffectiveDated.pick(rows, date)` resolves the row for any date; payroll stores the rule snapshot it used so old payrolls can be re-explained.
3. **Single source of truth for a day**: `classifyDay(employee, date)` (Phase 2) is used by attendance, leave and payroll. No module re-derives weekly off / holiday on its own.
4. **Pure calculators**: leave counting, accrual, sandwich, PF, ESI, PT, LWF, TDS, proration, comp-off, OT, gratuity, payslip totals are static pure functions (input records -> output records + trace). Services only load data and persist results.
5. **Audit everything**: settings, leave, attendance, salary, payroll writes go through `AuditLogService`.
6. **Maker-checker**: the user who processes a payroll run cannot approve it (checked in the service, not only the UI).
7. **No `eval`**: component formulas use a small safe expression parser (Phase 5), variables from a whitelist.
8. **Idempotent jobs**: accrual / carry-forward / payroll use unique keys (`employee, leave_type, period, txn_type`) so re-running never double-credits.
9. **Privacy**: PAN / Aadhaar / account numbers are masked in UI, API and logs; encrypted at rest in Phase 9 (key management is an open question, see ASSUMPTIONS).
10. **Phone-first UI**: same pattern as the DCR / Tour Plan pages (`css/app-ui.css`): bottom sheets on a phone, side panels on desktop.

## 3. Data model by phase

**Phase 1 - Foundation** (`101_hr_foundation.sql`)
`hr_company`, `hr_branch` (state, timezone), `hr_department`, `hr_grade`, `hr_cost_center`, `hr_employee_type` (permanent, probation, contract, fixed-term, intern, consultant with PF / ESI / leave / probation flags), `hr_emp_employment` (+ statutory extras: previous PF / EPS membership, disability, tax regime, PF member since), `hr_role`, `hr_role_permission`, `hr_emp_role`, `hr_permission_def`, `hr_audit_log`, `hr_setting_def`, `hr_setting`.

**Phase 2 - Work calendar** : `hr_work_type`, `hr_work_type_day_rule` (weekday + nth-week rules), `hr_shift`, `hr_roster`, `hr_emp_work_type` (effective history), `hr_holiday_calendar` (links the existing `holiday_master`), `hr_attendance_day`, `hr_attendance_punch`, `hr_attendance_summary`, `hr_regularization`, `hr_attendance_lock`.

**Phase 3 - Leave** : `hr_leave_type` (+ rule columns), `hr_leave_policy_override`, `hr_leave_application`, `hr_leave_application_day`, `hr_leave_ledger`, `hr_leave_clubbing_rule`, `hr_blackout_date`, `hr_leave_job_run`.

**Phase 4 - Comp-off** : `hr_compoff_request`, `hr_compoff_lot` (qty, expiry), consumption rows in the ledger (FIFO).

**Phase 5 - Salary** : `hr_component`, `hr_salary_template`, `hr_salary_template_component`, `hr_emp_salary` (effective-dated revisions), `hr_emp_salary_component`.

**Phase 6 - Statutory** : `hr_pf_rule`, `hr_esi_rule`, `hr_pt_slab`, `hr_lwf_rule`, `hr_tax_slab`, `hr_tax_param`, `hr_tax_declaration`, `hr_bonus_rule`, `hr_gratuity_rule`, `hr_ot_rule`, `hr_loan`, `hr_loan_emi`, `hr_reimbursement`.

**Phase 7 - Payroll** : `hr_payroll_run`, `hr_payroll_employee`, `hr_payroll_line`, `hr_payroll_trace`, `hr_payroll_hold`, `hr_fnf`.

**Phase 8 - Outputs** : `hr_payslip_snapshot` (immutable JSON + PDF path), `hr_bank_format`, export logs.

**Phase 9** : notifications, portal endpoints, encryption, performance indexes.

## 4. Code layout

```
in.opt.sfa.tenant.hr
  common/      EffectiveDated, Money, Rounding, AuditLogService, HrAccessService
  setting/     HrSettingService, HrSettingController   (effective-dated key / value)
  org/         company, branch, department, grade, cost centre, employee type CRUD
  employee/    HR employment + statutory extras
  access/      roles, permission matrix
  calc/        pure calculators (leave, pf, esi, pt, tds ...)   <- unit tested
  leave/ attendance/ payroll/ report/   (later phases)
static/hr/     hr-setup.html, hr-settings.html, hr-roles.html ... (later: leave, payroll, portal)
```

## 5. API (Phase 1) - all under `/api/hr`

`GET/POST /company`, `GET/POST /branches`, `/departments`, `/grades`, `/cost-centers`, `/employee-types`, `POST .../delete`;
`GET /settings` (catalogue + current values), `GET /settings/{key}/history`, `POST /settings` (new effective-dated value, reason required);
`GET /roles`, `POST /roles`, `GET /roles/matrix`, `POST /roles/{id}/permissions`, `GET/POST /employees/{id}/roles`;
`GET/POST /employees/{id}/employment`;
`GET /audit` (filter by entity / employee / user / date).

## 6. Screens (Phase 1)

* **HR Setup** : company profile (legal names, PAN, TAN, PF / ESI codes, FY start, leave-year mode, payroll cycle), branches (state), departments, grades, cost centres, employee types - one phone-friendly page with tabs.
* **HR Settings** : grouped settings list, "change from <date>" with reason, history timeline.
* **Roles & Access** : roles, permission matrix, assign roles to employees.
* **Employee HR profile** : employment + statutory extras (opened from the existing Employee page).
* **Audit log** viewer.

## 7. Testing

JUnit 5 (added to `pom.xml`). Pure calculator tests per phase (the brief asks for >= 90 % on calculation modules). Phase 1 tests: effective-date resolution (gaps, overlaps, open-ended, future rows), setting validation / typing, audit diff, role permission union, maker-checker rule.

## 8. Phase order and review stops

1 Foundation -> 2 Work calendar -> 3 Leave engine -> 4 Comp-off -> 5 Salary engine -> 6 Statutory engine -> 7 Payroll run -> 8 Outputs -> 9 Portal & polish.
**I stop after every phase** with: migrations to run, seed data, tests, how to try it, and the open questions of the next phase.
