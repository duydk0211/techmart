package com.techmart.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techmart.shared.constant.HeaderConstants;
import com.techmart.shared.constant.ResponseCode;
import com.techmart.shared.dto.ErrorResponse;
import com.techmart.shared.i18n.MessageUtils;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> ipBuckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public RateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if ("/api/v1/auth/login".equals(request.getRequestURI()) && "POST".equalsIgnoreCase(request.getMethod())) {
            String clientIp = resolveClientIp(request);
            Bucket bucket = ipBuckets.computeIfAbsent(clientIp, k -> createNewBucket());

            if (!bucket.tryConsume(1)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setHeader("Retry-After", "60");
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                String traceId = MDC.get(HeaderConstants.CORRELATION_ID);
                if (traceId == null) {
                    traceId = request.getHeader(HeaderConstants.CORRELATION_ID);
                }

                ErrorResponse error = ErrorResponse.builder()
                        .status(HttpStatus.TOO_MANY_REQUESTS.value())
                        .code(ResponseCode.RATE_LIMIT_EXCEEDED.getCode())
                        .message(MessageUtils.getMessage(ResponseCode.RATE_LIMIT_EXCEEDED))
                        .timestamp(Instant.now())
                        .path(request.getRequestURI())
                        .traceId(traceId)
                        .build();

                response.getWriter().write(objectMapper.writeValueAsString(error));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(5)
                .refillGreedy(5, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
