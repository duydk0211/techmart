package com.techmart.core.authorization;

import com.techmart.shared.constant.HeaderConstants;
import com.techmart.shared.constant.ResponseCode;
import com.techmart.shared.exception.CoreException;
import com.techmart.shared.security.TechmartPreauthorize;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Arrays;

@Aspect
@Component
@Order(1)
public class TechmartPreauthorizeAspect {

    private static final Logger log = LoggerFactory.getLogger(TechmartPreauthorizeAspect.class);

    @Value("${techmart.security.internal-secret:techmart_internal_secret_2026}")
    private String internalSecret;

    @Around("@annotation(com.techmart.shared.security.TechmartPreauthorize) || @within(com.techmart.shared.security.TechmartPreauthorize)")
    public Object enforceAuthorization(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Class<?> targetClass = joinPoint.getTarget() != null ? joinPoint.getTarget().getClass() : signature.getDeclaringType();
        Method targetMethod = AopUtils.getMostSpecificMethod(method, targetClass);
        TechmartPreauthorize techmartPreauthorize = AnnotationUtils.findAnnotation(targetMethod, TechmartPreauthorize.class);
        if (techmartPreauthorize == null) {
            techmartPreauthorize = AnnotationUtils.findAnnotation(method, TechmartPreauthorize.class);
        }
        if (techmartPreauthorize == null) {
            techmartPreauthorize = AnnotationUtils.findAnnotation(targetClass, TechmartPreauthorize.class);
        }
        if (techmartPreauthorize == null) {
            return joinPoint.proceed();
        }
        HttpServletRequest request = resolveCurrentRequest();

        if ("TECHMART_GW".equalsIgnoreCase(techmartPreauthorize.gateway())) {
            String incomingSecret = request != null ? request.getHeader(HeaderConstants.X_INTERNAL_SECRET) : null;
            if (incomingSecret == null || !incomingSecret.equals(internalSecret)) {
                log.warn("[TechmartPreauthorizeAspect][enforceAuthorization] Unauthorized gateway invocation attempt on method: {}",
                        joinPoint.getSignature().toShortString());
                throw new CoreException(ResponseCode.INVALID_INTERNAL_SECRET);
            }
        }

        UserContext.UserInfo currentUser = UserContext.getCurrentUser();
        if (techmartPreauthorize.authenticated()) {
            if (currentUser == null || currentUser.userId() == null) {
                log.warn("[TechmartPreauthorizeAspect][enforceAuthorization] Unauthenticated invocation on method: {}",
                        joinPoint.getSignature().toShortString());
                throw new CoreException(ResponseCode.UNAUTHORIZED);
            }
        }

        String[] requiredRoles = techmartPreauthorize.roles();
        if (requiredRoles.length > 0) {
            if (currentUser == null || currentUser.role() == null) {
                log.warn("[TechmartPreauthorizeAspect][enforceAuthorization] Missing role on method: {}",
                        joinPoint.getSignature().toShortString());
                throw new CoreException(ResponseCode.ACCESS_DENIED);
            }

            boolean hasRole = Arrays.stream(requiredRoles)
                    .anyMatch(r -> r.equalsIgnoreCase(currentUser.role()));
            if (!hasRole) {
                log.warn("[TechmartPreauthorizeAspect][enforceAuthorization] Role mismatch: user has '{}', required one of {} on method: {}",
                        currentUser.role(), Arrays.toString(requiredRoles), joinPoint.getSignature().toShortString());
                throw new CoreException(ResponseCode.ACCESS_DENIED);
            }
        }

        return joinPoint.proceed();
    }

    private HttpServletRequest resolveCurrentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }
}
