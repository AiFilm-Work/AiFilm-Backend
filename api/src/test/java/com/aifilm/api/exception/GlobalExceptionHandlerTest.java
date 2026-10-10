package com.aifilm.api.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

// 핸들러를 따로 @Import하지 않는다. @WebMvcTest가 @RestControllerAdvice를 슬라이스에 포함하는지도 함께 검증된다.
@WebMvcTest(ExceptionTestController.class)
class GlobalExceptionHandlerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("BusinessException은 담긴 ErrorReason의 상태, 코드, 메시지로 응답한다")
	void businessException() throws Exception {
		mockMvc.perform(get("/test/exceptions/business"))
			.andExpect(failResponse(409, "TEST_409_1"))
			.andExpect(jsonPath("$.message").value("테스트 리소스가 이미 존재합니다."));
	}

	@Test
	@DisplayName("처리하지 못한 예외는 500으로 응답하고 내부 메시지를 노출하지 않는다")
	void unexpectedException() throws Exception {
		String body = mockMvc.perform(get("/test/exceptions/unexpected"))
			.andExpect(failResponse(500, "COMMON_500_1"))
			.andExpect(jsonPath("$.message").value("서버 내부 오류가 발생했습니다."))
			.andReturn()
			.getResponse()
			.getContentAsString(StandardCharsets.UTF_8);

		assertThat(body).doesNotContain("내부 구현 정보");
	}

	@Test
	@DisplayName("없는 경로를 요청하면 404 공통 응답을 반환한다")
	void noResourceFound() throws Exception {
		mockMvc.perform(get("/test/exceptions/none"))
			.andExpect(failResponse(404, "COMMON_404_1"));
	}

	@Test
	@DisplayName("지원하지 않는 메서드로 요청하면 405 공통 응답과 함께 Allow 헤더를 유지한다")
	void methodNotAllowed() throws Exception {
		mockMvc.perform(post("/test/exceptions/business"))
			.andExpect(failResponse(405, "COMMON_405_1"))
			.andExpect(header().string(HttpHeaders.ALLOW, containsString("GET")));
	}

	@Test
	@DisplayName("지원하지 않는 Content-Type으로 요청하면 415 공통 응답을 반환한다")
	void unsupportedMediaType() throws Exception {
		mockMvc.perform(post("/test/exceptions/json")
				.contentType(MediaType.TEXT_PLAIN)
				.content("aifilm"))
			.andExpect(failResponse(415, "COMMON_415_1"));
	}

	// 실패 응답 공통 형태: 본문 status와 HTTP 상태가 같고, data는 null로 포함되며, errors는 없다.
	private static ResultMatcher failResponse(int httpStatus, String code) {
		return ResultMatcher.matchAll(
			status().is(httpStatus),
			jsonPath("$.status").value(httpStatus),
			jsonPath("$.code").value(code),
			jsonPath("$.data").value(nullValue()),
			jsonPath("$.errors").doesNotExist());
	}

}
