package com.autoarticle.exception;

import com.autoarticle.util.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;

import java.util.stream.Collectors;

/**
 * 全局异常处理。
 * JSON 接口：返回统一 {@link Result}，code 遵循架构文档错误码表，
 * HTTP 状态码与 code 对应（404->1004、400->1001、500->9000），
 * 且不回显底层 SQL/堆栈细节。
 * HTML 请求：返回对应错误视图。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public Object handleNotFound(ResourceNotFoundException ex, HttpServletRequest request, HttpServletResponse response) {
        log.warn("Resource not found: {}", ex.getMessage());
        ErrorCode errorCode = ErrorCode.NOT_FOUND;
        response.setStatus(errorCode.getHttpStatus().value());
        return buildBody(request, errorCode, ex.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Object handleValidation(BindException ex, HttpServletRequest request, HttpServletResponse response) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(msg -> msg != null && !msg.isBlank())
                .collect(Collectors.joining("; "));
        if (message.isEmpty()) {
            message = ErrorCode.PARAM_INVALID.getDefaultMessage();
        }
        log.warn("Validation error: {}", message);
        ErrorCode errorCode = ErrorCode.PARAM_INVALID;
        response.setStatus(errorCode.getHttpStatus().value());
        return buildBody(request, errorCode, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Object handleUnreadableBody(HttpMessageNotReadableException ex, HttpServletRequest request,
                                       HttpServletResponse response) {
        log.warn("Malformed request body: {}", ex.getMessage());
        ErrorCode errorCode = ErrorCode.PARAM_INVALID;
        response.setStatus(errorCode.getHttpStatus().value());
        return buildBody(request, errorCode, "请求体格式错误，请检查请求参数");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Object handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request,
                                     HttpServletResponse response) {
        log.warn("Parameter type mismatch [{}]: {}", ex.getName(), ex.getMessage());
        ErrorCode errorCode = ErrorCode.PARAM_INVALID;
        response.setStatus(errorCode.getHttpStatus().value());
        return buildBody(request, errorCode, "参数格式错误: " + ex.getName());
    }

    @ExceptionHandler(BusinessException.class)
    public Object handleBusiness(BusinessException ex, HttpServletRequest request, HttpServletResponse response) {
        log.warn("Business error: {}", ex.getMessage());
        ErrorCode errorCode = ex.getErrorCode();
        response.setStatus(errorCode.getHttpStatus().value());
        return buildBody(request, errorCode, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Object handleGeneral(Exception ex, HttpServletRequest request, HttpServletResponse response) {
        log.error("Unexpected error", ex);
        ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;
        response.setStatus(errorCode.getHttpStatus().value());
        return buildBody(request, errorCode, errorCode.getDefaultMessage());
    }

    private Object buildBody(HttpServletRequest request, ErrorCode errorCode, String message) {
        if (isHtmlRequest(request)) {
            String view = errorCode.getHttpStatus().value() == HttpServletResponse.SC_NOT_FOUND
                    ? "error/404" : "error/500";
            ModelAndView mav = new ModelAndView(view);
            mav.addObject("message", message);
            return mav;
        }
        return Result.error(errorCode.getCode(), message);
    }

    private boolean isHtmlRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        if (accept != null) {
            return accept.contains("text/html");
        }
        String uri = request.getRequestURI();
        return uri.endsWith(".html") || uri.equals("/") || uri.equals("/dashboard");
    }
}
