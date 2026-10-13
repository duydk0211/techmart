package com.techmart.core.aspect;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techmart.core.authorization.UserContext;
import com.techmart.core.entity.idempotency.IdempotencyKeyEntity;
import com.techmart.core.entity.idempotency.IdempotencyStatus;
import com.techmart.core.service.command.idempotency.IdempotencyService;
import com.techmart.shared.annotation.Idempotent;
import com.techmart.shared.constant.HeaderConstants;
import com.techmart.shared.constant.ResponseCode;
import com.techmart.shared.exception.CoreException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring AOP Aspect enforcing Idempotency for annotated endpoints.
 */
@Aspect
@Component
@Order(100)
public class IdempotencyAspect {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyAspect.class);

    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public IdempotencyAspect(IdempotencyService idempotencyService, ObjectMapper objectMapper) {
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(com.techmart.shared.annotation.Idempotent) || @within(com.techmart.shared.annotation.Idempotent)")
    public Object handleIdempotency(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Class<?> targetClass = joinPoint.getTarget() != null ? joinPoint.getTarget().getClass() : signature.getDeclaringType();
        Method targetMethod = AopUtils.getMostSpecificMethod(method, targetClass);

        Idempotent idempotent = AnnotationUtils.findAnnotation(targetMethod, Idempotent.class);
        if (idempotent == null) {
            idempotent = AnnotationUtils.findAnnotation(method, Idempotent.class);
        }
        if (idempotent == null) {
            idempotent = AnnotationUtils.findAnnotation(targetClass, Idempotent.class);
        }
        if (idempotent == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = resolveCurrentRequest();
        if (request == null) {
            return joinPoint.proceed();
        }

        String idempotencyKey = request.getHeader(HeaderConstants.IDEMPOTENCY_KEY);
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return joinPoint.proceed();
        }

        UUID userId = resolveCurrentUserId(request);
        if (userId == null) {
            throw new CoreException(ResponseCode.UNAUTHORIZED, "User authentication required for idempotent operations");
        }

        String requestHash = computeRequestHash(joinPoint);
        log.info("[IdempotencyAspect][handleIdempotency] Intercepted key: {} for user: {} with hash: {}", idempotencyKey, userId, requestHash);

        Optional<IdempotencyKeyEntity> existingOpt = idempotencyService.findByKeyAndUserId(idempotencyKey, userId);
        if (existingOpt.isPresent()) {
            IdempotencyKeyEntity existing = existingOpt.get();
            boolean isExpired = existing.getExpiresAt().isBefore(Instant.now());

            if (!isExpired) {
                if (existing.getStatus() == IdempotencyStatus.IN_PROGRESS) {
                    log.warn("[IdempotencyAspect][handleIdempotency] Concurrent operation in progress for key: {}", idempotencyKey);
                    throw new CoreException(ResponseCode.IDEMPOTENCY_KEY_IN_PROGRESS);
                } else if (existing.getStatus() == IdempotencyStatus.COMPLETED) {
                    if (!requestHash.equals(existing.getRequestHash())) {
                        log.warn("[IdempotencyAspect][handleIdempotency] Payload mismatch for idempotency key: {}", idempotencyKey);
                        throw new CoreException(ResponseCode.IDEMPOTENCY_KEY_PAYLOAD_MISMATCH);
                    }

                    log.info("[IdempotencyAspect][handleIdempotency] Replaying cached response for idempotency key: {}", idempotencyKey);
                    idempotencyService.incrementBlockedRequests();
                    return buildReplayResponse(joinPoint, existing);
                }
            }
        }

        IdempotencyKeyEntity record = idempotencyService.createOrUpdateInProgress(
                idempotencyKey,
                userId,
                request.getRequestURI(),
                requestHash,
                idempotent.expireHours()
        );

        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable t) {
            log.error("[IdempotencyAspect][handleIdempotency] Execution failed for key: {}, marking status FAILED", idempotencyKey);
            idempotencyService.markFailed(record.getId());
            throw t;
        }

        int statusCode = 200;
        String responseBodyJson = "";

        if (result instanceof ResponseEntity<?> responseEntity) {
            statusCode = responseEntity.getStatusCode().value();
            Object body = responseEntity.getBody();
            if (body != null) {
                responseBodyJson = objectMapper.writeValueAsString(body);
            }
        } else if (result != null) {
            responseBodyJson = objectMapper.writeValueAsString(result);
        }

        idempotencyService.markCompleted(record.getId(), statusCode, responseBodyJson);
        return result;
    }

    private HttpServletRequest resolveCurrentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    private UUID resolveCurrentUserId(HttpServletRequest request) {
        UserContext.UserInfo currentUser = UserContext.getCurrentUser();
        if (currentUser != null && currentUser.userId() != null) {
            return currentUser.userId();
        }
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            try {
                return UUID.fromString(userIdHeader);
            } catch (Exception ignored) {
                // Ignore parse exception
            }
        }
        return null;
    }

    private String computeRequestHash(ProceedingJoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            Object payloadObj = null;
            for (Object arg : args) {
                if (arg != null && !(arg instanceof HttpServletRequest) && !(arg instanceof HttpServletResponse)) {
                    payloadObj = arg;
                    break;
                }
            }
            String payloadString = payloadObj != null ? objectMapper.writeValueAsString(payloadObj) : "";
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(payloadString.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            log.warn("[IdempotencyAspect][computeRequestHash] Error calculating hash: {}", e.getMessage());
            return "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        }
    }

    private Object buildReplayResponse(ProceedingJoinPoint joinPoint, IdempotencyKeyEntity record) throws Exception {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Class<?> returnType = signature.getReturnType();
        HttpStatus status = HttpStatus.resolve(record.getResponseStatus() != null ? record.getResponseStatus() : 200);
        if (status == null) {
            status = HttpStatus.OK;
        }

        String json = record.getResponseBody();
        if (ResponseEntity.class.isAssignableFrom(returnType)) {
            Type genericReturnType = signature.getMethod().getGenericReturnType();
            Object body = null;
            if (genericReturnType instanceof ParameterizedType paramType) {
                Type[] typeArgs = paramType.getActualTypeArguments();
                if (typeArgs.length > 0 && json != null && !json.isBlank()) {
                    JavaType javaType = objectMapper.getTypeFactory().constructType(typeArgs[0]);
                    body = objectMapper.readValue(json, javaType);
                }
            }
            if (body == null && json != null && !json.isBlank()) {
                body = objectMapper.readValue(json, Object.class);
            }
            return ResponseEntity.status(status).body(body);
        } else {
            if (json != null && !json.isBlank()) {
                return objectMapper.readValue(json, returnType);
            }
            return null;
        }
    }
}
