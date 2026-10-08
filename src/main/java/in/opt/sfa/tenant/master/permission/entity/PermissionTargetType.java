package in.opt.sfa.tenant.master.permission.entity;

/**
 * What a PermissionAssignment row targets. Resolution precedence (most specific
 * wins) is EMP_ID > DESIGNATION > EMP_LEVEL — e.g. a whole designation can be
 * granted a permission while one specific emp_id in that designation is denied.
 */
public enum PermissionTargetType {
    EMP_ID,
    DESIGNATION,
    EMP_LEVEL
}
