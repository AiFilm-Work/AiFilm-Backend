package com.aifilm;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.aifilm.support.IntegrationTest;

// Actuator는 health만 노출하고, 실행 모드와 상관없이 헬스체크가 동작하는지 확인한다.
@IntegrationTest
@AutoConfigureMockMvc
class ActuatorEndpointTest {

	@Nested
	@DisplayName("기본 설정(API + Worker)")
	class DefaultMode {

		@Autowired
		private MockMvc mockMvc;

		@Test
		@DisplayName("/actuator/health는 200과 UP 상태만 응답한다")
		void healthUp() throws Exception {
			mockMvc.perform(get("/actuator/health"))
				.andExpectAll(
					status().isOk(),
					jsonPath("$.status").value("UP"),
					jsonPath("$.components").doesNotExist());
		}

		@ParameterizedTest
		@ValueSource(strings = {"/actuator/env", "/actuator/beans"})
		@DisplayName("health 외의 엔드포인트는 열리지 않는다")
		void otherEndpointsNotExposed(String path) throws Exception {
			mockMvc.perform(get(path))
				.andExpect(status().isNotFound());
		}
	}

	@Nested
	@DisplayName("Worker만 켰을 때")
	@TestPropertySource(properties = {"aifilm.api.enabled=false", "aifilm.worker.enabled=true"})
	class WorkerOnly {

		@Autowired
		private MockMvc mockMvc;

		@Test
		@DisplayName("API 모듈 없이도 /actuator/health로 상태를 확인할 수 있다")
		void healthUp() throws Exception {
			mockMvc.perform(get("/actuator/health"))
				.andExpectAll(
					status().isOk(),
					jsonPath("$.status").value("UP"));
		}
	}

}
