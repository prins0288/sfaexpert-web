package in.opt.sfa.security;

import in.opt.sfa.exception.ForbiddenException;
import in.opt.sfa.tenant.master.permission.service.PermissionService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Enforces @RequiresPermission on controller methods. Checked AFTER
 * TenantAuthFilter has populated UserContext, so the identity here is already
 * verified from the JWT signature.
 */
@Aspect
@Component
public class PermissionAspect {

    private final PermissionService permissionService;

    public PermissionAspect(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Around("@annotation(in.opt.sfa.security.RequiresPermission) || @within(in.opt.sfa.security.RequiresPermission)")
    public Object checkPermission(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method method = sig.getMethod();
        RequiresPermission ann = AnnotationUtils.findAnnotation(method, RequiresPermission.class);
        if (ann == null) {
            ann = AnnotationUtils.findAnnotation(pjp.getTarget().getClass(), RequiresPermission.class);
        }
        if (ann != null && !permissionService.isAllowed(ann.value())) {
            throw new ForbiddenException("Requires permission " + ann.value());
        }
        return pjp.proceed();
    }
}
