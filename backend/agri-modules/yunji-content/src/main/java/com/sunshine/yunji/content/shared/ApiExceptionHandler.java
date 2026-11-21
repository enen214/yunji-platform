package com.sunshine.yunji.content.shared;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.core.annotation.Order;

@RestControllerAdvice(basePackages="com.sunshine.yunji.content")
@Order(-100)
public class ApiExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> business(BusinessException e) {
        return ResponseEntity.status(e.status).body(new ApiResponse<>(e.status,e.getMessage(),null));
    }
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> conflict(Exception e) {
        return ResponseEntity.status(409).body(new ApiResponse<>(409,"对象重复、被引用或状态发生变化",null));
    }
    @ExceptionHandler({org.springframework.web.bind.MethodArgumentNotValidException.class,org.springframework.http.converter.HttpMessageNotReadableException.class})
    public ResponseEntity<ApiResponse<Void>> body(Exception e) {
        return ResponseEntity.unprocessableEntity().body(new ApiResponse<>(422,"请求正文不符合约束",null));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> unexpected(Exception e) {
        org.slf4j.LoggerFactory.getLogger(ApiExceptionHandler.class).error("Business API failed: {}",e.getClass().getSimpleName());
        return ResponseEntity.status(500).body(new ApiResponse<>(500,"服务暂时不可用，请联系管理员",null));
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> status(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(new ApiResponse<>(e.getStatusCode().value(),e.getReason(),null));
    }
    @ExceptionHandler({ConstraintViolationException.class,HandlerMethodValidationException.class,MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Void>> invalid(Exception e) {
        return ResponseEntity.unprocessableEntity().body(new ApiResponse<>(422,"请求参数不符合约束",null));
    }
}
