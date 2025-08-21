package com.greencloud.server.controllers;

import com.greencloud.server.dto.requests.LogoutRequest;
import com.greencloud.server.dto.requests.SignInRequest;
import com.greencloud.server.dto.requests.SignUpRequest;
import com.greencloud.server.dto.responses.APIResponse;
import com.greencloud.server.services.auth.AuthService;
import com.greencloud.server.services.dto.AuthTokens;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<APIResponse<Void>> signUp(@Valid @RequestBody SignUpRequest req) {
        authService.signUp(req);
        return ResponseEntity.ok(APIResponse.success("signed up"));
    }

    // 로그인
    @PostMapping("/signin")
    public ResponseEntity<APIResponse<Map<String, Object>>> signIn(@Valid @RequestBody SignInRequest req) {
        AuthTokens tokens = authService.signIn(req);
        return ResponseEntity.ok(APIResponse.success(
                "login success",
                Map.of(
                        "accessToken", tokens.accessToken(),
                        "refreshToken", tokens.refreshToken(),
                        "tokenType", tokens.tokenType()
                )
        ));
    }

    // 토큰 갱신
    @PostMapping("/refresh")
    public ResponseEntity<APIResponse<Map<String, Object>>> refresh(@RequestParam("refreshToken") String refreshToken) {
        AuthTokens tokens = authService.refresh(refreshToken);
        return ResponseEntity.ok(APIResponse.success(
                "token refreshed",
                Map.of(
                        "accessToken", tokens.accessToken(),
                        "refreshToken", tokens.refreshToken(),
                        "tokenType", tokens.tokenType()
                )
        ));
    }

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<APIResponse<Void>> logoutBody(HttpServletRequest request,
                                                        @Valid @RequestBody LogoutRequest req) {
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        String accessToken = (auth != null && auth.startsWith("Bearer ")) ? auth.substring(7) : "";
        authService.logout(accessToken, req.getRefreshToken());
        return ResponseEntity.ok(APIResponse.success("logged out"));
    }

    // 이메일 중복 확인
    @GetMapping("/check-email")
    public ResponseEntity<APIResponse<Map<String, Object>>> checkEmail(@RequestParam @NotBlank @Email String email) {
        boolean available = authService.isEmailAvailable(email);
        // APIResponse에 맞춰 아래 둘 중 하나 사용

        // (A) static 팩토리 메서드가 있을 때
        return ResponseEntity.ok(
                APIResponse.success("ok", Map.of("available", available))
        );
    }
}