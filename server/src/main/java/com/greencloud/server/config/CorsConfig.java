package com.greencloud.server.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@EnableConfigurationProperties(CorsProps.class)
public class CorsConfig implements WebMvcConfigurer {

    private final CorsProps props;

    public CorsConfig(CorsProps props) {
        this.props = props;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // application-*.yml 에서 설정값들을 읽어옴
        List<String> patterns = props.getAllowedOriginPatterns() != null && !props.getAllowedOriginPatterns().isEmpty()
                ? props.getAllowedOriginPatterns()
                : List.of("http://localhost:*", "http://127.0.0.1:*");

        List<String> methods = props.getAllowedMethods() != null && !props.getAllowedMethods().isEmpty()
                ? props.getAllowedMethods()
                : List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");

        List<String> headers = props.getAllowedHeaders() != null && !props.getAllowedHeaders().isEmpty()
                ? props.getAllowedHeaders()
                : List.of("*");

        List<String> exposed = props.getExposedHeaders() != null ? props.getExposedHeaders() : List.of();

        // yml에 정의된 설정을 "/api/**" 경로에 적용하도록 변경
        registry.addMapping("/api/**")
                .allowedOriginPatterns(patterns.toArray(String[]::new))
                .allowedMethods(methods.toArray(String[]::new))
                .allowedHeaders(headers.toArray(String[]::new))
                .exposedHeaders(exposed.toArray(String[]::new))
                .allowCredentials(Boolean.TRUE.equals(props.getAllowCredentials()))
                .maxAge(props.getMaxAge() != null ? props.getMaxAge() : 3600L);

        // Actuator 경로에 대한 CORS 설정 추가
        registry.addMapping("/actuator/**")
                .allowedOriginPatterns(patterns.toArray(String[]::new))
                .allowedMethods("GET") // health는 GET만 필요
                .allowedHeaders(headers.toArray(String[]::new))
                .allowCredentials(Boolean.TRUE.equals(props.getAllowCredentials()))
                .maxAge(props.getMaxAge() != null ? props.getMaxAge() : 3600L);
    }
}