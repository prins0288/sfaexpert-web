package in.opt.sfa.tenant.master.permission.entity;

/**
 * What a PermissionAssignment row targets. Resolution precedence (most specific
 * wins) is EMP_ID > DESIGNATION > HIERARCHY_LEVEL — e.g. a whole designation can be
 * granted a permission while one specific emp_id in that designation is denied.
 */
public enum PermissionTargetType {
    EMP_ID,
    /** target_value = designation_master.id */
    DESIGNATION,
    /** target_value = designation_master.hierarchy_level */
    HIERARCHY_LEVEL
}
