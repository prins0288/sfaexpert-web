-- ===========================================================================
-- Per-employee menu LAYOUT overrides (reorder + re-parent), on top of the
-- per-employee visibility overrides. One menu_visibility row per
-- (emp_id, menu_item_id) now also carries an optional parent_id / sort_order:
--   effective parent = override.parent_id if set, else menu_item.parent_id
--   effective order  = override.sort_order if set, else menu_item.sort_order
-- so an admin can move an item (e.g. Route-Area Map from Geography to Mapping)
-- for ONE employee, and only that employee's sidebar reflects it.
--
-- Set by the Menu Visibility page (Admin/Manager only). NULL parent_id/sort_order
-- means "follow the base menu".
--
-- Run once per tenant DB (acme_db, globex_db, initech_db, sfa_demo).
-- ===========================================================================

-- ALTER TABLE menu_visibility
--   ADD COLUMN parent_id  BIGINT NULL AFTER menu_item_id,
--   ADD COLUMN sort_order INT    NULL AFTER parent_id;

-- (applied programmatically to all tenant DBs; kept here for the record.)
SELECT 'menu_visibility now carries per-employee parent_id + sort_order overrides' AS note;
