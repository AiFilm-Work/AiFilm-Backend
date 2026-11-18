package com.aifilm.api.support;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// ControllerTestConfigurationTest에서 슬라이스 대상으로 쓰는 테스트 전용 컨트롤러
@RestController
class TestPingController {

    @GetMapping("/test/ping")
    public String ping() {
        return "pong";
    }

}
