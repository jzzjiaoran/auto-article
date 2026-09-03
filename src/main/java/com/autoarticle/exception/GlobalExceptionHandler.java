package com.autoarticle.exception;

import com.autoarticle.util.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.ModelAndView;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Object handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        if (isHtmlRequest(request)) {
            ModelAndView mav = new ModelAndView("error/404");
            mav.addObject("message", ex.getMessage());
            return mav;
        }
        return Result.error(404, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Object handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("Validation error: {}", message);
        if (isHtmlRequest(request)) {
            ModelAndView mav = new ModelAndView("error/500");
            mav.addObject("message", message);
            return mav;
        }
        return Result.error(400, message);
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Object handleBusiness(BusinessException ex, HttpServletRequest request) {
        log.warn("Business error: {}", ex.getMessage());
        if (isHtmlRequest(request)) {
            ModelAndView mav = new ModelAndView("error/500");
            mav.addObject("message", ex.getMessage());
            return mav;
        }
        return Result.error(400, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Object handleGeneral(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error", ex);
        if (isHtmlRequest(request)) {
            ModelAndView mav = new ModelAndView("error/500");
            mav.addObject("message", "服务器内部错误，请稍后重试");
            return mav;
        }
        return Result.error(500, "服务器内部错误");
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
