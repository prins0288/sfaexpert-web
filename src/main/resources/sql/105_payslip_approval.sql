-- 105: payslips are approved one by one: a slip made later in an approved month needs its own approval. Safe to re-run.

USE acme_db;

SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'hr_payslip' AND column_name = 'approved_at');
SET @s := IF(@c = 0, 'ALTER TABLE hr_payslip ADD COLUMN generated_by VARCHAR(40) NULL AFTER emp_id, ADD COLUMN approved_by VARCHAR(40) NULL, ADD COLUMN approved_at DATETIME NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
UPDATE hr_payslip s JOIN hr_payroll_run r ON r.id = s.run_id SET s.generated_by = COALESCE(s.generated_by, r.generated_by), s.approved_by = r.approved_by, s.approved_at = r.approved_at WHERE r.status IN ('APPROVED', 'PAID') AND s.approved_at IS NULL;
UPDATE hr_payslip s JOIN hr_payroll_run r ON r.id = s.run_id SET s.generated_by = r.generated_by WHERE s.generated_by IS NULL;

USE globex_db;

SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'hr_payslip' AND column_name = 'approved_at');
SET @s := IF(@c = 0, 'ALTER TABLE hr_payslip ADD COLUMN generated_by VARCHAR(40) NULL AFTER emp_id, ADD COLUMN approved_by VARCHAR(40) NULL, ADD COLUMN approved_at DATETIME NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
UPDATE hr_payslip s JOIN hr_payroll_run r ON r.id = s.run_id SET s.generated_by = COALESCE(s.generated_by, r.generated_by), s.approved_by = r.approved_by, s.approved_at = r.approved_at WHERE r.status IN ('APPROVED', 'PAID') AND s.approved_at IS NULL;
UPDATE hr_payslip s JOIN hr_payroll_run r ON r.id = s.run_id SET s.generated_by = r.generated_by WHERE s.generated_by IS NULL;

USE initech_db;

SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'hr_payslip' AND column_name = 'approved_at');
SET @s := IF(@c = 0, 'ALTER TABLE hr_payslip ADD COLUMN generated_by VARCHAR(40) NULL AFTER emp_id, ADD COLUMN approved_by VARCHAR(40) NULL, ADD COLUMN approved_at DATETIME NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
UPDATE hr_payslip s JOIN hr_payroll_run r ON r.id = s.run_id SET s.generated_by = COALESCE(s.generated_by, r.generated_by), s.approved_by = r.approved_by, s.approved_at = r.approved_at WHERE r.status IN ('APPROVED', 'PAID') AND s.approved_at IS NULL;
UPDATE hr_payslip s JOIN hr_payroll_run r ON r.id = s.run_id SET s.generated_by = r.generated_by WHERE s.generated_by IS NULL;

USE sfa_demo;

SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'hr_payslip' AND column_name = 'approved_at');
SET @s := IF(@c = 0, 'ALTER TABLE hr_payslip ADD COLUMN generated_by VARCHAR(40) NULL AFTER emp_id, ADD COLUMN approved_by VARCHAR(40) NULL, ADD COLUMN approved_at DATETIME NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
UPDATE hr_payslip s JOIN hr_payroll_run r ON r.id = s.run_id SET s.generated_by = COALESCE(s.generated_by, r.generated_by), s.approved_by = r.approved_by, s.approved_at = r.approved_at WHERE r.status IN ('APPROVED', 'PAID') AND s.approved_at IS NULL;
UPDATE hr_payslip s JOIN hr_payroll_run r ON r.id = s.run_id SET s.generated_by = r.generated_by WHERE s.generated_by IS NULL;
