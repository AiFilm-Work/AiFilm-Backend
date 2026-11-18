package com.aifilm.api.config;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

// CORS 확인용 테스트 컨트롤러만 올린다. 지정하지 않으면 모든 컨트롤러가 올라와 서비스가 필요한 컨트롤러 때문에 뜨지 않는다.
@WebMvcTest(controllers = CorsTestController.class,
        properties = "aifilm.cors.allowed-origins=https://aifilm.life, http://localhost:3000")
class CorsConfigTest {

    private static final String ALLOWED_ORIGIN = "https://aifilm.life";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("허용한 출처의 사전 요청에는 허용 출처와 메서드를 응답한다")
    void preflightFromAllowedOrigin() throws Exception {
        mockMvc.perform(options("/test/cors")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type"))
                .andExpectAll(
                        status().isOk(),
                        header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN),
                        header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("PATCH")),
                        header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @Test
    @DisplayName("목록의 다른 출처도 앞뒤 공백 없이 허용된다")
    void preflightFromSecondOrigin() throws Exception {
        mockMvc.perform(options("/test/cors")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpectAll(
                        status().isOk(),
                        header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"));
    }

    @Test
    @DisplayName("허용하지 않은 출처의 사전 요청은 403으로 거절한다")
    void preflightFromUnknownOrigin() throws Exception {
        mockMvc.perform(options("/test/cors")
                        .header(HttpHeaders.ORIGIN, "https://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpectAll(
                        status().isForbidden(),
                        header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("허용한 출처의 실제 요청에도 허용 출처 헤더가 붙는다")
    void actualRequestFromAllowedOrigin() throws Exception {
        mockMvc.perform(get("/test/cors").header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN))
                .andExpectAll(
                        status().isOk(),
                        header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN));
    }

    @Test
    @DisplayName("Origin이 없는 요청(같은 출처, 앱)은 CORS 헤더 없이 그대로 처리한다")
    void requestWithoutOrigin() throws Exception {
        mockMvc.perform(get("/test/cors"))
                .andExpectAll(
                        status().isOk(),
                        header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

}
