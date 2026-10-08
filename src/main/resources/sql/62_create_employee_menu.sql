-- Wire the pre-seeded "Create Employee" menu leaf (Masters > Master Entry >
-- Create Employee, id=510 from 25_master_submenu_tree.sql) to its actual page
-- — it was created with page/href both NULL (a placeholder).
UPDATE company_menu_self
SET page = 'createemployee', href = 'master/create-employee.html'
WHERE id = 510;
