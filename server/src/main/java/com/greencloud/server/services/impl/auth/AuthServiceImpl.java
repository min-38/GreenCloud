package com.greencloud.server.services.impl.auth;

import com.greencloud.server.dto.requests.SignInRequest;
import com.greencloud.server.dto.requests.SignUpRequest;
import com.greencloud.server.exceptions.DuplicateEmailException;
import com.greencloud.server.exceptions.InvalidTokenException;
import com.greencloud.server.exceptions.UserNotFoundException;
import com.greencloud.server.models.Role;
import com.greencloud.server.models.User;
import com.greencloud.server.repositories.UserRepository;
import com.greencloud.server.security.jwt.JwtAuthenticationFilter;
import com.greencloud.server.security.jwt.JwtTokenProvider;
import com.greencloud.server.services.auth.AuthService;
import com.greencloud.server.services.dto.AuthTokens;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtTokenProvider jwt;
    private final StringRedisTemplate redis;

    @Override
    @Transactional
    public void signUp(SignUpRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new DuplicateEmailException(); // 커스텀 예외 (전역 핸들러에서 APIResponse로 변환)
        }
        User user = new User();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setPassword(encoder.encode(req.getPassword()));
        user.setRole(Role.USER);
        userRepository.save(user);
    }

    @Override
    public AuthTokens signIn(SignInRequest req) {
        authManager.authenticate(new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        User user = userRepository.findByEmail(req.getEmail()).orElseThrow(UserNotFoundException::new);

        String at = jwt.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String rt = jwt.generateRefreshToken(user.getId());
        redis.opsForValue().set("RT:" + user.getId(), rt, jwt.getRefreshExpSeconds(), TimeUnit.SECONDS);

        return AuthTokens.bearer(at, rt);
    }

    @Override
    @Transactional
    public AuthTokens refresh(String refreshToken) {
        Claims claims = jwt.parseClaims(refreshToken);
        if (!"refresh".equals(claims.get("typ"))) throw new InvalidTokenException();

        Long userId = Long.valueOf(claims.getSubject());
        String saved = redis.opsForValue().get("RT:" + userId);
        if (saved == null || !saved.equals(refreshToken)) throw new InvalidTokenException();

        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        String newAt = jwt.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String newRt = jwt.generateRefreshToken(user.getId());
        redis.opsForValue().set("RT:" + userId, newRt, jwt.getRefreshExpSeconds(), TimeUnit.SECONDS);

        return AuthTokens.bearer(newAt, newRt);
    }

    @Override
    @Transactional
    public void logout(String accessToken, String refreshToken) {
        // RT 삭제
        try {
            Claims rt = jwt.parseClaims(refreshToken);
            if ("refresh".equals(rt.get("typ"))) redis.delete("RT:" + rt.getSubject());
        } catch (Exception ignored) {}

        // AT 블랙리스트
        try {
            Claims at = jwt.parseClaims(accessToken);
            long ttl = Math.max(0, at.getExpiration().toInstant().getEpochSecond() - Instant.now().getEpochSecond());
            JwtAuthenticationFilter.blacklist(redis, accessToken, ttl);
        } catch (Exception ignored) {}
    }

    @Override
    public boolean isEmailAvailable(String email) {
        // true = 사용 가능, false = 이미 사용 중
        return !userRepository.existsByEmail(email);
    }
}
