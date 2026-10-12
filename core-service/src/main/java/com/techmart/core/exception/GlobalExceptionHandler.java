package com.techmart.core.exception;

import com.techmart.shared.constant.HeaderConstants;
import com.techmart.shared.constant.ResponseCode;
import com.techmart.shared.dto.ErrorResponse;
import com.techmart.shared.dto.FieldErrorItem;
import com.techmart.shared.exception.CoreException;
import com.techmart.shared.i18n.MessageUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CoreException.class)
    public ResponseEntity<ErrorResponse> handleCoreException(CoreException ex, HttpServletRequest request) {
        log.warn("[GlobalExceptionHandler][handleCoreException] CoreException occurred: code={}, message={}",
                ex.getResponseCode().getCode(), ex.getMessage());

        ResponseCode code = ex.getResponseCode();
        HttpStatus status = code.getHttpStatus();

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(status.value())
                .code(code.getCode())
                .message(MessageUtils.getMessage(code, ex.getArgs()))
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .traceId(request.getHeader(HeaderConstants.CORRELATION_ID))
                .build();

        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("[GlobalExceptionHandler][handleValidationException] Validation failed: {}", ex.getMessage());

        List<FieldErrorItem> fieldErrors = new ArrayList<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.add(FieldErrorItem.builder()
                    .field(fe.getField())
                    .rejectedValue(fe.getRejectedValue())
                    .message(fe.getDefaultMessage())
                    .build());
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .code(ResponseCode.VALIDATION_FAILED.getCode())
                .message(MessageUtils.getMessage(ResponseCode.VALIDATION_FAILED))
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .traceId(request.getHeader(HeaderConstants.CORRELATION_ID))
                .errors(fieldErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("[GlobalExceptionHandler][handleGenericException] Unhandled exception occurred: ", ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .code(ResponseCode.INTERNAL_SERVER_ERROR.getCode())
                .message(MessageUtils.getMessage(ResponseCode.INTERNAL_SERVER_ERROR))
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .traceId(request.getHeader(HeaderConstants.CORRELATION_ID))
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
