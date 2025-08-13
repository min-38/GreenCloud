package com.greencloud.server.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CorsConfigTest {

    @Autowired
    MockMvc mockMvc;

    private static final String ORIGIN = "http://localhost:5173";

    @Test
    @DisplayName("서버가 동작 중인지 확인. Status=UP이면 동작 중")
    void health_up() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("CORS Preflight(OPTIONS)는 허용되어 CORS 헤더를 반환한다")
    void cors_preflight_ok() throws Exception {
        mockMvc.perform(options("/files") // CORS 레벨에서 응답하므로 실제 Controller는 필요가 없음
                        .header("Origin", ORIGIN)
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization, Content-Type"))
                .andExpect(status().isOk()) // 서버가 허용하면 200 또는 204여야 함
                .andExpect(header().string("Access-Control-Allow-Origin", ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Methods", org.hamcrest.Matchers.containsString("GET")))
                .andExpect(header().string("Access-Control-Allow-Headers", org.hamcrest.Matchers.containsString("Authorization")));
    }

    @Test
    @DisplayName("실제 요청에도 CORS 헤더가 포함된다")
    void cors_actual_get_has_header() throws Exception {
        mockMvc.perform(get("/test") // 실제 Controller는 필요함. 아니면 404 나옴
                        .header("Origin", ORIGIN))
                .andExpect(status().isOk()) // 매핑이 없으면 404가 날 수 있으니, 필요시 컨트롤러 dummy 추가
                .andExpect(header().string("Access-Control-Allow-Origin", ORIGIN));
    }

    @Test
    @DisplayName("미허용 Origin의 Preflight(OPTIONS)는 403이고 CORS 헤더가 없다")
    void preflight_blocked_forbidden() throws Exception {
        mockMvc.perform(options("/test")
                        .header("Origin", "http://evil.example")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("미허용 Origin의 실제 요청에도 CORS 허용 헤더가 붙지 않는다")
    void actual_blocked_no_cors_headers() throws Exception {
        mockMvc.perform(get("/test")
                        .header("Origin", "http://www.example.com"))
                .andExpect(status().isForbidden())                // CORS 필터가 차단하면 403
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}