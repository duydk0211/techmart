package com.techmart.gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techmart.shared.constant.HeaderConstants;
import com.techmart.shared.constant.ResponseCode;
import com.techmart.shared.dto.ErrorResponse;
import com.techmart.shared.i18n.MessageUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
public class GatewayAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(GatewayAccessDeniedHandler.class);

    private final ObjectMapper objectMapper;

    public GatewayAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        log.warn("[GatewayAccessDeniedHandler][handle] Access denied to URI: {} message: {}",
                request.getRequestURI(), accessDeniedException.getMessage());

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        String traceId = MDC.get(HeaderConstants.CORRELATION_ID);
        if (traceId == null) {
            traceId = request.getHeader(HeaderConstants.CORRELATION_ID);
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.FORBIDDEN.value())
                .code(ResponseCode.ACCESS_DENIED.getCode())
                .message(MessageUtils.getMessage(ResponseCode.ACCESS_DENIED))
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .traceId(traceId)
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
