package com.aifilm.api.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

// CorsConfigTest에서 CORS 응답 헤더를 확인할 때 쓰는 테스트 전용 컨트롤러
@RestController
class CorsTestController {

    @GetMapping("/test/cors")
    public String get() {
        return "ok";
    }

    // 사전 요청(preflight)은 Access-Control-Request-Method에 맞는 핸들러가 있어야 처리된다
    @PostMapping("/test/cors")
    public String post() {
        return "ok";
    }

}
