package com.greencloud.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CORS 설정을 Security에 연결
                .cors(Customizer.withDefaults())
                // API 성격이므로 CSRF, 폼 로그인은 비활성화
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                .authorizeHttpRequests(auth -> auth
                        // 프리플라이트(미리 보내보는 요청) 전역 허용
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 헬스체크 무조건 허용
                        .requestMatchers("/actuator/health").permitAll()
                        // 나머지는 모두 허용
                        // TODO: 실제 보안 정책 적용 필요
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}
