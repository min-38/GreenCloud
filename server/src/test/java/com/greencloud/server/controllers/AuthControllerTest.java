package com.greencloud.server.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.greencloud.server.dto.requests.SignInRequest;
import com.greencloud.server.dto.requests.SignUpRequest;
import com.greencloud.server.exceptions.DuplicateEmailException;
import com.greencloud.server.exceptions.InvalidTokenException;
import com.greencloud.server.services.auth.AuthService;
import com.greencloud.server.services.dto.AuthTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@ExtendWith(MockitoExtension.class)
@Import(AuthControllerTest.TestSecurityConfig.class)
class AuthControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired AuthService authService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        AuthService authService() {
            return Mockito.mock(AuthService.class);
        }
    }

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(csrf -> csrf.disable()) // 필요시 유지/비활성
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll()); // ✅ 전부 허용
            return http.build();
        }
    }

    @BeforeEach
    void resetMocks() {
        Mockito.reset(authService); // ✅ 모든 이전 호출/스터빙 초기화
    }

    @Test
    @DisplayName("회원가입 200 OK")
    void signUp_ok() throws Exception {
        SignUpRequest req = new SignUpRequest();
        req.setUsername("username");
        req.setEmail("a@a.com");
        req.setPassword("pw12345678@#2Q");
        req.setPassword2("pw12345678@#2Q");

        doNothing().when(authService).signUp(any(SignUpRequest.class));

        mvc.perform(post("/api/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("signed up")));
        verify(authService).signUp(any(SignUpRequest.class));
    }

    @Test
    @DisplayName("회원가입 - 실패(중복 이메일)")
    void signUp_fail_duplicate_email() throws Exception {
        SignUpRequest req = new SignUpRequest();
        req.setUsername("username");
        req.setEmail("a@a.com");
        req.setPassword("pw12345678@#2Q");
        req.setPassword2("pw12345678@#2Q");

        // 서비스가 중복 이메일 예외를 던지도록 스텁
        doThrow(new DuplicateEmailException())
                .when(authService).signUp(any(SignUpRequest.class));

        mvc.perform(post("/api/auth/signup")
                        .with(csrf()) // 보안 필터가 켜져있으면 필요
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isConflict())                  // 409
                .andExpect(jsonPath("$.success", is(false)))       // 실패
                .andExpect(jsonPath("$.message", is("email already used"))); // 예외 메시지

        verify(authService).signUp(any(SignUpRequest.class));
    }

    @Test
    @DisplayName("회원가입 - 실패(이메일 누락)")
    void signup_fail_email_required() throws Exception {
        var req = new SignUpRequest();
        req.setUsername("u");
        req.setEmail(""); // 누락
        req.setPassword("pw12345678@A");
        req.setPassword2("pw12345678@A");

        mvc.perform(post("/api/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("validation failed"))
                .andExpect(jsonPath("$.data.email").value("이메일을 입력해주세요."));
    }

    @Test
    @DisplayName("회원가입 - 실패(이메일 형식)")
    void signup_fail_email_format() throws Exception {
        var req = new SignUpRequest();
        req.setUsername("u");
        req.setEmail("not-email");
        req.setPassword("pw12345678@A");
        req.setPassword2("pw12345678@A");

        mvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.email").value("올바른 이메일 형식이 아닙니다."));
    }

    @Test
    @DisplayName("회원가입 - 실패(비밀번호 길이)")
    void signup_fail_password_length() throws Exception {
        var req = new SignUpRequest();
        req.setUsername("u");
        req.setEmail("a@a.com");
        req.setPassword("Short1@");
        req.setPassword2("Short1@");

        mvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.password").value("비밀번호는 8~24자 내외, 대문자/소문자/숫자/특수문자를 포함해야 합니다."));
    }

    @Test
    @DisplayName("회원가입 - 실패(비밀번호 조건)")
    void signup_fail_password_regex() throws Exception {
        var req = new SignUpRequest();
        req.setUsername("u");
        req.setEmail("a@a.com");
        req.setPassword("pw12345678");
        req.setPassword2("pw12345678");

        mvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.password").value("비밀번호는 8~24자 내외, 대문자/소문자/숫자/특수문자를 포함해야 합니다."));
    }

    @Test
    @DisplayName("회원가입 - 실패(비밀번호 길이 + 조건)")
    void signup_fail_password_length_and_regex() throws Exception {
        var req = new SignUpRequest();
        req.setUsername("u");
        req.setEmail("a@a.com");
        req.setPassword("12345678213123812301287389128397218397218921837128372813783721sfasaefsaef");
        req.setPassword2("12345678213123812301287389128397218397218921837128372813783721sfasaefsaef");

        mvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.password").value("비밀번호는 8~24자 내외, 대문자/소문자/숫자/특수문자를 포함해야 합니다."));
    }

    @Test
    @DisplayName("회원가입 - 실패(비밀번호 불일치)")
    void signup_fail_password_mismatch() throws Exception {
        var req = new SignUpRequest();
        req.setUsername("u");
        req.setEmail("a@a.com");
        req.setPassword("pw12345678@A");
        req.setPassword2("pw12345678@B");

        mvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.password2").value("비밀번호가 일치하지 않습니다."));
    }

    @Test
    @DisplayName("로그인 - 성공")
    void signIn_ok() throws Exception {
        when(authService.signIn(any(SignInRequest.class)))
                .thenReturn(AuthTokens.bearer("AT", "RT"));

        mvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("email","a@a.com","password","pw"))))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken", is("AT")))
                .andExpect(jsonPath("$.data.refreshToken", is("RT")))
                .andExpect(jsonPath("$.data.tokenType", is("Bearer")));
    }

    @Test
    @DisplayName("로그인 - 실패(필수값 누락)")
    void signin_fail_required() throws Exception {
        var req = new SignInRequest();
        req.setEmail(""); // 누락
        req.setPassword(""); // 누락

        mvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.email").value("이메일을 입력해주세요."))
                .andExpect(jsonPath("$.data.password").value("비밀번호를 입력해주세요."));
    }

    @Test
    @DisplayName("리프레시 성공")
    void refresh_ok() throws Exception {
        when(authService.refresh("RT")).thenReturn(AuthTokens.bearer("AT2","RT2"));

        mvc.perform(post("/api/auth/refresh").param("refreshToken","RT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken", is("AT2")))
                .andExpect(jsonPath("$.data.refreshToken", is("RT2")));
    }

    @Test
    @DisplayName("리프레시 - 실패(유효하지 않은 토큰)")
    void refresh_fail_invalid_token() throws Exception {
        String invalidToken = "some-invalid-or-expired-token";

        // authService.refresh가 InvalidTokenException을 던지도록 설정
        when(authService.refresh(invalidToken))
                .thenThrow(new InvalidTokenException());

        mvc.perform(post("/api/auth/refresh")
                        .param("refreshToken", invalidToken))
                .andDo(print())
                .andExpect(status().isUnauthorized()) // 401
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("invalid token"));

        verify(authService).refresh(invalidToken);
    }

    @Test
    @DisplayName("로그아웃 성공 (본문 JSON)")
    void logout_ok() throws Exception {
        // {"refreshToken":"RT"} 본문 전송
        var body = Map.of("refreshToken", "RT");

        mvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer AT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("logged out"));

        verify(authService).logout(eq("AT"), eq("RT"));
    }

    @Test
    void checkEmail_available_true() throws Exception {
        when(authService.isEmailAvailable("a@a.com")).thenReturn(true);

        mvc.perform(get("/api/auth/check-email").param("email", "a@a.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.available").value(true));
    }
}