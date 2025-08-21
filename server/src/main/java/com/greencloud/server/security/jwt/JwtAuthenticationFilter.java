package com.greencloud.server.security.jwt;

import com.greencloud.server.repositories.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTP;
    private final UserRepository userRepo;
    private final StringRedisTemplate redis;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String auth = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(auth) && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);

            // 블랙리스트(로그아웃) 체크
            if (redis.opsForValue().get("BL:" + token) == null) {
                try {
                    Claims claims = jwtTP.parseClaims(token);
                    Long userId = Long.valueOf(claims.getSubject());

                    var userOpt = userRepo.findById(userId);
                    if (userOpt.isPresent()) {
                        var u = userOpt.get();
                        var principal = new org.springframework.security.core.userdetails.User(
                                u.getEmail(), u.getPassword(),
                                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + u.getRole()))
                        );
                        var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                } catch (Exception ignored) { /* 토큰 문제면 익명으로 진행 */ }
            }
        }
        chain.doFilter(req, res);
    }

    public static void blacklist(StringRedisTemplate redis, String token, long secondsLeft) {
        if (secondsLeft > 0) {
            redis.opsForValue().set("BL:" + token, "1", Duration.ofSeconds(secondsLeft));
        }
    }
}
