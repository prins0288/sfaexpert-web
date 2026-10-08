package in.opt.sfa.security;

import java.lang.annotation.*;

/**
 * Put on a controller method (or a whole controller class) to gate it behind a
 * permission code resolved by PermissionService — checked against the calling
 * user's emp_id, designation and emp_level, in that precedence order. If the
 * company has not configured permission_assignment for this code at all, the
 * call is allowed by default (see PermissionService.resolve).
 *
 * Usage:  @RequiresPermission("USER_CREATE")
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermission {
    String value();
}
