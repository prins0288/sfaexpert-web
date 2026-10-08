-- Rename app_user -> user_login_master (COMMON db, sfa_central).
-- JPA entity AppUser now maps to user_login_master. Columns are unchanged
-- (id, username, password, tenant_id, role, emp_id[NOT NULL], enabled, last_login_at).
RENAME TABLE app_user TO user_login_master;
