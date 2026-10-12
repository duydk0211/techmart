package com.techmart.core.authorization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techmart.shared.constant.HeaderConstants;
import com.techmart.shared.constant.ResponseCode;
import com.techmart.shared.dto.ErrorResponse;
import com.techmart.shared.i18n.MessageUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Component
public class InternalAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(InternalAuthFilter.class);

    @Value("${techmart.security.internal-secret:techmart_internal_secret_2026}")
    private String internalSecret;

    private final ObjectMapper objectMapper;

    public InternalAuthFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String secretHeader = request.getHeader(HeaderConstants.X_INTERNAL_SECRET);

        if (secretHeader == null || !secretHeader.equals(internalSecret)) {
            log.warn("[InternalAuthFilter][doFilterInternal] Rejected unauthorized internal request to URI: {} from remote: {}",
                    request.getRequestURI(), request.getRemoteAddr());

            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            ErrorResponse error = ErrorResponse.builder()
                    .status(HttpStatus.FORBIDDEN.value())
                    .code(ResponseCode.INVALID_INTERNAL_SECRET.getCode())
                    .message(MessageUtils.getMessage(ResponseCode.INVALID_INTERNAL_SECRET))
                    .timestamp(Instant.now())
                    .path(request.getRequestURI())
                    .traceId(request.getHeader(HeaderConstants.CORRELATION_ID))
                    .build();

            response.getWriter().write(objectMapper.writeValueAsString(error));
            return;
        }

        String userIdHeader = request.getHeader("X-User-Id");
        String roleHeader = request.getHeader("X-User-Role");
        String emailHeader = request.getHeader("X-User-Email");

        if (userIdHeader != null && !userIdHeader.isBlank()) {
            try {
                UserContext.setCurrentUser(new UserContext.UserInfo(UUID.fromString(userIdHeader), roleHeader, emailHeader));
            } catch (Exception e) {
                log.warn("[InternalAuthFilter][doFilterInternal] Failed to parse X-User-Id: {}", userIdHeader);
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }
}
