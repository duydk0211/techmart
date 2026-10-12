package com.techmart.shared.exception;

import com.techmart.shared.constant.ResponseCode;
import lombok.Getter;

/**
 * System-wide standard runtime exception associated with ResponseCode and i18n arguments.
 */
@Getter
public class CoreException extends RuntimeException {

    private final ResponseCode responseCode;
    private final Object[] args;

    public CoreException(ResponseCode responseCode, Object... args) {
        super(responseCode != null ? responseCode.getCode() : "UNKNOWN_ERROR");
        this.responseCode = responseCode;
        this.args = args;
    }

    public CoreException(ResponseCode responseCode, String customMessage, Object... args) {
        super(customMessage);
        this.responseCode = responseCode;
        this.args = args;
    }

    public CoreException(ResponseCode responseCode, Throwable cause, Object... args) {
        super(responseCode != null ? responseCode.getCode() : "UNKNOWN_ERROR", cause);
        this.responseCode = responseCode;
        this.args = args;
    }
}
