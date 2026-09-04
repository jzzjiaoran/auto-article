package com.autoarticle.exception;

/**
 * 业务异常。默认视为请求参数/业务校验不通过（HTTP 400，code 1001）；
 * 领域相关错误可显式指定更精确的 {@link ErrorCode}（如 1005 冲突、2001 AI 失败、2003 发布失败等）。
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(String message) {
        this(ErrorCode.PARAM_INVALID, message);
    }

    public BusinessException(String message, Throwable cause) {
        this(ErrorCode.PARAM_INVALID, message, cause);
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
