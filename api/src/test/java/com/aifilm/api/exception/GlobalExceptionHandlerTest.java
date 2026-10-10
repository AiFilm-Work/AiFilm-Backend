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
			.andExpectAll(failResponse(409, "TEST_409_1"))
			.andExpect(jsonPath("$.message").value("테스트 리소스가 이미 존재합니다."));
	}

	@Test
	@DisplayName("처리하지 못한 예외는 500으로 응답하고 내부 메시지를 노출하지 않는다")
	void unexpectedException() throws Exception {
		String body = mockMvc.perform(get("/test/exceptions/unexpected"))
			.andExpectAll(failResponse(500, "COMMON_500_1"))
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
			.andExpectAll(failResponse(404, "COMMON_404_1"));
	}

	@Test
	@DisplayName("지원하지 않는 메서드로 요청하면 405 공통 응답과 함께 Allow 헤더를 유지한다")
	void methodNotAllowed() throws Exception {
		mockMvc.perform(post("/test/exceptions/business"))
			.andExpectAll(failResponse(405, "COMMON_405_1"))
			.andExpect(header().string(HttpHeaders.ALLOW, containsString("GET")));
	}

	@Test
	@DisplayName("지원하지 않는 Content-Type으로 요청하면 415 공통 응답을 반환한다")
	void unsupportedMediaType() throws Exception {
		mockMvc.perform(post("/test/exceptions/json")
				.contentType(MediaType.TEXT_PLAIN)
				.content("aifilm"))
			.andExpectAll(failResponse(415, "COMMON_415_1"));
	}

	@Test
	@DisplayName("요청 본문 @Valid 검증에 실패하면 400과 필드별 오류를 반환한다")
	void methodArgumentNotValid() throws Exception {
		mockMvc.perform(post("/test/exceptions/body")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"\"}"))
			.andExpectAll(validationFailResponse("COMMON_400_1"))
			.andExpect(jsonPath("$.errors.length()").value(1))
			.andExpect(jsonPath("$.errors[0].field").value("name"))
			.andExpect(jsonPath("$.errors[0].reason").isNotEmpty());
	}

	@Test
	@DisplayName("경로 변수 제약 조건을 위반하면 400과 파라미터 이름이 담긴 오류를 반환한다")
	void handlerMethodValidation() throws Exception {
		mockMvc.perform(get("/test/exceptions/path/{id}", 0))
			.andExpectAll(validationFailResponse("COMMON_400_1"))
			.andExpect(jsonPath("$.errors[0].field").value("id"))
			.andExpect(jsonPath("$.errors[0].reason").isNotEmpty());
	}

	@Test
	@DisplayName("필수 파라미터가 없으면 400과 누락된 파라미터 오류를 반환한다")
	void missingRequestParameter() throws Exception {
		mockMvc.perform(get("/test/exceptions/param"))
			.andExpectAll(validationFailResponse("COMMON_400_1"))
			.andExpect(jsonPath("$.errors[0].field").value("count"))
			.andExpect(jsonPath("$.errors[0].reason").value("필수 값입니다."));
	}

	@Test
	@DisplayName("파라미터 타입이 맞지 않으면 400을 반환하고 보낸 값은 응답에 담지 않는다")
	void typeMismatch() throws Exception {
		String body = mockMvc.perform(get("/test/exceptions/param").param("count", "aifilm-value"))
			.andExpectAll(validationFailResponse("COMMON_400_1"))
			.andExpect(jsonPath("$.errors[0].field").value("count"))
			.andExpect(jsonPath("$.errors[0].reason").value("형식이 올바르지 않습니다."))
			.andReturn()
			.getResponse()
			.getContentAsString(StandardCharsets.UTF_8);

		assertThat(body).doesNotContain("aifilm-value");
	}

	@Test
	@DisplayName("요청 본문 JSON을 읽을 수 없으면 400 별도 코드로 응답하고 필드 오류는 담지 않는다")
	void httpMessageNotReadable() throws Exception {
		mockMvc.perform(post("/test/exceptions/body")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":"))
			.andExpectAll(failResponse(400, "COMMON_400_2"));
	}

	// 검증 실패 응답 공통 형태: 400이고 data는 null로 포함되며, errors에 필드별 오류가 담긴다.
	private static ResultMatcher[] validationFailResponse(String code) {
		return new ResultMatcher[] {
			status().isBadRequest(),
			jsonPath("$.status").value(400),
			jsonPath("$.code").value(code),
			jsonPath("$.data").value(nullValue()),
			jsonPath("$.errors").isArray()
		};
	}

	// 실패 응답 공통 형태: 본문 status와 HTTP 상태가 같고, data는 null로 포함되며, errors는 없다.
	private static ResultMatcher[] failResponse(int httpStatus, String code) {
		return new ResultMatcher[] {
			status().is(httpStatus),
			jsonPath("$.status").value(httpStatus),
			jsonPath("$.code").value(code),
			jsonPath("$.data").value(nullValue()),
			jsonPath("$.errors").doesNotExist()
		};
	}

}
