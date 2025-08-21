package com.greencloud.server.exceptions;

import com.greencloud.server.dto.responses.APIResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /***********************
     * 400 Error Handling
     ***********************/
    // @Valid body 검증 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<APIResponse<Map<String, String>>> handleMethodArgNotValid(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        return ResponseEntity.badRequest()
                .body(APIResponse.fail("validation failed", errors));
    }

    // @RequestParam/@PathVariable 등 제약 위반
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<APIResponse<Map<String, String>>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v ->
                errors.put(v.getPropertyPath().toString(), v.getMessage())
        );
        return ResponseEntity.badRequest()
                .body(APIResponse.fail("validation failed", errors));
    }

    // JSON 파싱 실패, 타입 불일치 등
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<APIResponse<?>> handleNotReadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(APIResponse.fail("malformed request body"));
    }

    // 필수 파라미터 누락
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<APIResponse<?>> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest()
                .body(APIResponse.fail("missing parameter: " + ex.getParameterName()));
    }

    /***********************
     * 403 Error Handling
     ***********************/
    // 권한 없음
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<APIResponse<?>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(APIResponse.fail("forbidden"));
    }

    /***********************
     * 500 Error Handling
     ***********************/
    // 그 외 모든 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<APIResponse<?>> handleEtc(Exception ex) {
        // 로그는 여기서 남겨줘 (ex.printStackTrace() 대신 로거 사용 권장)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(APIResponse.fail("internal error"));
    }
}