package com.aifilm;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import com.aifilm.api.ApiModuleConfig;
import com.aifilm.api.ApiScanMarker;
import com.aifilm.support.IntegrationTest;
import com.aifilm.worker.WorkerModuleConfig;
import com.aifilm.worker.WorkerScanMarker;

// 실행 모드 스위치(aifilm.api.enabled, aifilm.worker.enabled) 조합에 따라 api·worker 모듈의 빈이 등록되는지 검증한다.
// 모듈마다 진입 설정 클래스와, 그 모듈 패키지 스캔으로만 등록되는 테스트 전용 마커 빈이 함께 켜지고 꺼지는지 확인한다.
@IntegrationTest
class AiFilmApplicationModeTest {

	@Nested
	@DisplayName("스위치를 따로 주지 않은 기본 설정")
	class DefaultSwitches {

		@Autowired
		private ApplicationContext context;

		@Test
		@DisplayName("application.yml의 기본값으로 API와 Worker가 모두 켜진다")
		void bothModulesEnabledByDefault() {
			assertModule(context, ApiModuleConfig.class, ApiScanMarker.class, true);
			assertModule(context, WorkerModuleConfig.class, WorkerScanMarker.class, true);
		}
	}

	@Nested
	@DisplayName("API와 Worker를 모두 켰을 때")
	@TestPropertySource(properties = {"aifilm.api.enabled=true", "aifilm.worker.enabled=true"})
	class BothEnabled {

		@Autowired
		private ApplicationContext context;

		@Test
		@DisplayName("두 모듈의 빈이 모두 등록된다")
		void bothModulesEnabled() {
			assertModule(context, ApiModuleConfig.class, ApiScanMarker.class, true);
			assertModule(context, WorkerModuleConfig.class, WorkerScanMarker.class, true);
		}
	}

	@Nested
	@DisplayName("API만 켰을 때")
	@TestPropertySource(properties = {"aifilm.api.enabled=true", "aifilm.worker.enabled=false"})
	class ApiOnly {

		@Autowired
		private ApplicationContext context;

		@Test
		@DisplayName("Worker 모듈의 빈이 등록되지 않는다")
		void workerModuleDisabled() {
			assertModule(context, ApiModuleConfig.class, ApiScanMarker.class, true);
			assertModule(context, WorkerModuleConfig.class, WorkerScanMarker.class, false);
		}
	}

	@Nested
	@DisplayName("Worker만 켰을 때")
	@TestPropertySource(properties = {"aifilm.api.enabled=false", "aifilm.worker.enabled=true"})
	class WorkerOnly {

		@Autowired
		private ApplicationContext context;

		@Test
		@DisplayName("API 모듈의 빈이 등록되지 않는다")
		void apiModuleDisabled() {
			assertModule(context, ApiModuleConfig.class, ApiScanMarker.class, false);
			assertModule(context, WorkerModuleConfig.class, WorkerScanMarker.class, true);
		}
	}

	private static void assertModule(ApplicationContext context, Class<?> moduleConfig, Class<?> scanMarker, boolean enabled) {
		assertThat(hasBean(context, moduleConfig)).as("%s 등록 여부", moduleConfig.getSimpleName()).isEqualTo(enabled);
		assertThat(hasBean(context, scanMarker)).as("%s 등록 여부", scanMarker.getSimpleName()).isEqualTo(enabled);
	}

	private static boolean hasBean(ApplicationContext context, Class<?> type) {
		return context.getBeanNamesForType(type).length > 0;
	}

}
