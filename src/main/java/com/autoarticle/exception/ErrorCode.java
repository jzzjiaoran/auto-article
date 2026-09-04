package com.autoarticle.exception;

import org.springframework.http.HttpStatus;

/**
 * 错误码定义，与架构文档「错误码定义」一节保持一致。
 * code 0 表示成功；业务错误码范围 1001~9000。
 */
public enum ErrorCode {

    SUCCESS(0, HttpStatus.OK, "success"),
    PARAM_INVALID(1001, HttpStatus.BAD_REQUEST, "参数校验失败"),
    UNAUTHORIZED(1002, HttpStatus.UNAUTHORIZED, "未认证或未登录"),
    FORBIDDEN(1003, HttpStatus.FORBIDDEN, "无权限"),
    NOT_FOUND(1004, HttpStatus.NOT_FOUND, "资源不存在"),
    CONFLICT(1005, HttpStatus.CONFLICT, "资源冲突"),
    AI_SERVICE_ERROR(2001, HttpStatus.INTERNAL_SERVER_ERROR, "AI 服务调用失败"),
    SCRAPE_ERROR(2002, HttpStatus.INTERNAL_SERVER_ERROR, "热点抓取失败"),
    PUBLISH_ERROR(2003, HttpStatus.INTERNAL_SERVER_ERROR, "平台发布失败"),
    CREDENTIAL_ERROR(2004, HttpStatus.INTERNAL_SERVER_ERROR, "平台账号凭据无效"),
    INTERNAL_ERROR(9000, HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误，请稍后重试");

    private final int code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(int code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public int getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
