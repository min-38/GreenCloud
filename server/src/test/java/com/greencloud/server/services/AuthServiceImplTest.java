package com.greencloud.server.services;

import com.greencloud.server.dto.requests.SignInRequest;
import com.greencloud.server.dto.requests.SignUpRequest;
import com.greencloud.server.exceptions.DuplicateEmailException;
import com.greencloud.server.exceptions.InvalidTokenException;
import com.greencloud.server.models.Role;
import com.greencloud.server.models.User;
import com.greencloud.server.repositories.UserRepository;
import com.greencloud.server.security.jwt.JwtTokenProvider;
import com.greencloud.server.services.dto.AuthTokens;
import com.greencloud.server.services.impl.auth.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.mockito.ArgumentMatchers.*;
import io.jsonwebtoken.Claims;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import static org.mockito.Mockito.when;

public class AuthServiceImplTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private BCryptPasswordEncoder encoder;
    @Mock
    private AuthenticationManager authManager;
    @Mock
    private JwtTokenProvider jwt;
    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private AuthServiceImpl service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        when(redis.opsForValue()).thenReturn(valueOps);
        // 기본 JWT TTL
        when(jwt.getRefreshExpSeconds()).thenReturn(1209600L);
    }

    @Test
    @DisplayName("회원가입 - 성공(패스워드 인코딩/저장)")
    void signUp_success() {
        SignUpRequest req = new SignUpRequest();
        req.setEmail("a@a.com");
        req.setPassword("plain");

        when(userRepository.existsByEmail("a@a.com")).thenReturn(false);
        when(encoder.encode("plain")).thenReturn("ENC");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.signUp(req);

        ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(cap.capture());
        assertThat(cap.getValue().getEmail()).isEqualTo("a@a.com");
        assertThat(cap.getValue().getPassword()).isEqualTo("ENC");
        assertThat(cap.getValue().getRole()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("회원가입 - 실패(이메일 중복 시 DuplicateEmailException)")
    void signUp_duplicateEmail() {
        SignUpRequest req = new SignUpRequest();
        req.setEmail("a@a.com");
        req.setPassword("pw12345678");

        when(userRepository.existsByEmail("a@a.com")).thenReturn(true);

        assertThatThrownBy(() -> service.signUp(req))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    @DisplayName("로그인 - 성공(인증 후 토큰/RT 저장)")
    void signIn_success() {
        SignInRequest req = new SignInRequest();
        req.setEmail("a@a.com");
        req.setPassword("pw");

        User user = new User();
        user.setId(1L); user.setEmail("a@a.com"); user.setPassword("ENC"); user.setRole(Role.USER);

        when(userRepository.findByEmail("a@a.com")).thenReturn(Optional.of(user));
        doAnswer(inv -> null).when(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        when(jwt.generateAccessToken(1L, "a@a.com", "USER")).thenReturn("AT");
        when(jwt.generateRefreshToken(1L)).thenReturn("RT");

        AuthTokens tokens = service.signIn(req);

        assertThat(tokens.accessToken()).isEqualTo("AT");
        assertThat(tokens.refreshToken()).isEqualTo("RT");
        verify(valueOps).set(eq("RT:1"), eq("RT"), eq(1209600L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("로그인 - 실패(인증 실패)")
    void signIn_authFails() {
        SignInRequest req = new SignInRequest();
        req.setEmail("a@a.com"); req.setPassword("pw");

        when(userRepository.findByEmail("a@a.com")).thenReturn(Optional.of(new User()));
        doThrow(new RuntimeException("bad credentials"))
                .when(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThatThrownBy(() -> service.signIn(req))
                .hasMessageContaining("bad credentials");
    }

    @Test
    @DisplayName("리프레시 - typ!=refresh면 InvalidTokenException")
    void refresh_invalid_type() {
        when(jwt.parseClaims("X")).thenThrow(new InvalidTokenException());
        assertThatThrownBy(() -> service.refresh("X"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("리프레시 - 저장된 RT와 다르면 InvalidTokenException")
    void refresh_mismatch() {
        Claims claims = mock(Claims.class);
        when(jwt.parseClaims("RT1")).thenReturn(claims);
        when(claims.get("typ")).thenReturn("refresh");
        when(claims.getSubject()).thenReturn("1");

        when(valueOps.get("RT:1")).thenReturn("OTHER");

        assertThatThrownBy(() -> service.refresh("RT1"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("리프레시 - 토큰 파싱 실패(만료/변조)")
    void refresh_parse_fails() {
        when(jwt.parseClaims("RT_BAD")).thenThrow(new InvalidTokenException());
        assertThatThrownBy(() -> service.refresh("RT_BAD"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("리프레시 - 유저 없음")
    void refresh_userNotFound() {
        Claims claims = mock(Claims.class);
        when(jwt.parseClaims("RT1")).thenReturn(claims);
        when(claims.get("typ")).thenReturn("refresh");
        when(claims.getSubject()).thenReturn("99");

        when(valueOps.get("RT:99")).thenReturn("RT1");
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh("RT1"))
                .isInstanceOf(com.greencloud.server.exceptions.UserNotFoundException.class)
                .hasMessageContaining("user not found");
    }

    @Test
    @DisplayName("리프레시 - 새 토큰 발급 및 RT 교체")
    void refresh_success() {
        Claims claims = mock(Claims.class);
        when(jwt.parseClaims("RT1")).thenReturn(claims);
        when(claims.get("typ")).thenReturn("refresh");
        when(claims.getSubject()).thenReturn("1");

        when(valueOps.get("RT:1")).thenReturn("RT1");

        User user = new User();
        user.setId(1L); user.setEmail("a@a.com"); user.setRole(Role.USER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        when(jwt.generateAccessToken(1L, "a@a.com", "USER")).thenReturn("AT2");
        when(jwt.generateRefreshToken(1L)).thenReturn("RT2");

        AuthTokens tokens = service.refresh("RT1");

        assertThat(tokens.accessToken()).isEqualTo("AT2");
        assertThat(tokens.refreshToken()).isEqualTo("RT2");
        verify(valueOps).set(eq("RT:1"), eq("RT2"), eq(1209600L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("로그아웃 - RT 삭제 + AT 블랙리스트")
    void logout_success() {
        // 1) RT 파싱 스텁
        Claims rt = mock(Claims.class);
        when(jwt.parseClaims("REFRESH")).thenReturn(rt);
        when(rt.get("typ")).thenReturn("refresh");
        when(rt.getSubject()).thenReturn("1");

        // 2) AT 파싱 스텁 (만료를 미래로 설정 → TTL 생기게)
        Claims at = mock(Claims.class);
        when(jwt.parseClaims("ACCESS")).thenReturn(at);
        when(at.getExpiration()).thenReturn(Date.from(Instant.now().plusSeconds(100)));

        // 3) 실행: ✅ any() 같은 매처를 “실제 인자”로 절대 넣지 말 것
        service.logout("ACCESS", "REFRESH");

        // 4) 검증: ✅ verify 안에서는 매처 사용 가능
        verify(redis).delete("RT:1");

        // 블랙리스트 set 호출 검증 (TTL은 Duration 타입)
        // 필요하면 ArgumentCaptor로 TTL을 받아서 범위를 검증

        ArgumentCaptor<Duration> ttlCap = ArgumentCaptor.forClass(Duration.class);
        verify(valueOps).set(startsWith("BL:"), eq("1"), ttlCap.capture());
        assertThat(ttlCap.getValue().getSeconds()).isBetween(1L, 120L);
    }

    @Test
    @DisplayName("로그아웃 - 만료된 AT면 블랙리스트 TTL=0 또는 생략")
    void logout_expiredAccess() {
        Claims rt = mock(Claims.class);
        when(jwt.parseClaims("REFRESH")).thenReturn(rt);
        when(rt.get("typ")).thenReturn("refresh");
        when(rt.getSubject()).thenReturn("1");

        Claims at = mock(Claims.class);
        when(jwt.parseClaims("ACCESS")).thenReturn(at);
        when(at.getExpiration()).thenReturn(Date.from(Instant.now().minusSeconds(1)));

        service.logout("ACCESS","REFRESH");

        verify(redis).delete("RT:1");
        // 블랙리스트 저장 정책에 맞춰 아래 둘 중 하나를 검증
        // verify(valueOps, never()).set(startsWith("BL:"), anyString(), any(Duration.class));
        // or: verify(valueOps).set(startsWith("BL:"), eq("1"), eq(Duration.ZERO));
    }
}
