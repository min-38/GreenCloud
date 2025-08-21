package com.greencloud.server.exceptions;

import com.greencloud.server.controllers.AuthController;
import com.greencloud.server.dto.responses.APIResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// AuthController 에서 발생하는 예외만 처리하도록 범위를 지정
@RestControllerAdvice(assignableTypes = AuthController.class)
public class AuthExceptionHandler {

    /***********************
     * 401 Error Handling
     ***********************/
    // 로그인 실패 (이메일/비번 불일치)
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<APIResponse<?>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(APIResponse.fail("invalid email or password"));
    }

    // 토큰 문제 (서명 실패/만료/형식 오류 등)
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<APIResponse<?>> handleInvalidToken(InvalidTokenException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(APIResponse.fail(ex.getMessage()));
    }

    /***********************
     * 404 Error Handling
     ***********************/
    // 유저 없음
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<APIResponse<?>> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(APIResponse.fail(ex.getMessage()));
    }

    /***********************
     * 409 Error Handling
     ***********************/
    // 이메일 중복
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<APIResponse<?>> handleDuplicate(DuplicateEmailException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(APIResponse.fail(ex.getMessage()));
    }


}
