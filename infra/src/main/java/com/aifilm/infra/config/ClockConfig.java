package com.aifilm.infra.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 현재 시각을 Clock 빈으로 주입받아, 시각에 따라 결과가 달라지는 로직(배너 노출 기간 등)을 테스트에서 고정할 수 있게 한다.
// API와 Worker가 함께 쓰므로 두 모듈이 모두 의존하는 infra에 둔다. 서버 기본 시간대(Asia/Seoul)를 쓴다.
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

	@Bean
	Clock clock() {
		return Clock.systemDefaultZone();
	}

}
