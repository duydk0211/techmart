package com.techmart.gateway.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techmart.shared.constant.HeaderConstants;
import com.techmart.shared.constant.ResponseCode;
import com.techmart.shared.dto.ErrorResponse;
import com.techmart.shared.exception.CoreException;
import com.techmart.shared.i18n.MessageUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;

@RestControllerAdvice
public class GatewayExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GatewayExceptionHandler.class);

    private final ObjectMapper objectMapper;

    public GatewayExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<Object> handleRestClientResponseException(RestClientResponseException ex, HttpServletRequest request) {
        log.warn("[GatewayExceptionHandler][handleRestClientResponseException] Core error status: {} body: {}",
                ex.getStatusCode(), ex.getResponseBodyAsString());

        try {
            ErrorResponse errorResponse = objectMapper.readValue(ex.getResponseBodyAsString(), ErrorResponse.class);
            return ResponseEntity.status(ex.getStatusCode()).body(errorResponse);
        } catch (Exception parseException) {
            ErrorResponse fallback = ErrorResponse.builder()
                    .status(ex.getStatusCode().value())
                    .code(ResponseCode.INTERNAL_SERVER_ERROR.getCode())
                    .message(ex.getStatusText())
                    .timestamp(Instant.now())
                    .path(request.getRequestURI())
                    .traceId(resolveTraceId(request))
                    .build();
            return ResponseEntity.status(ex.getStatusCode()).body(fallback);
        }
    }

    @ExceptionHandler(CoreException.class)
    public ResponseEntity<ErrorResponse> handleCoreException(CoreException ex, HttpServletRequest request) {
        log.warn("[GatewayExceptionHandler][handleCoreException] CoreException occurred: code={}", ex.getResponseCode().getCode());

        ResponseCode code = ex.getResponseCode();
        HttpStatus status = code.getHttpStatus();

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(status.value())
                .code(code.getCode())
                .message(MessageUtils.getMessage(code, ex.getArgs()))
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .traceId(resolveTraceId(request))
                .build();

        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("[GatewayExceptionHandler][handleGenericException] Unhandled gateway exception: ", ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .code(ResponseCode.INTERNAL_SERVER_ERROR.getCode())
                .message(MessageUtils.getMessage(ResponseCode.INTERNAL_SERVER_ERROR))
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .traceId(resolveTraceId(request))
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    private String resolveTraceId(HttpServletRequest request) {
        String traceId = MDC.get(HeaderConstants.CORRELATION_ID);
        if (traceId == null) {
            traceId = request.getHeader(HeaderConstants.CORRELATION_ID);
        }
        return traceId;
    }
}
