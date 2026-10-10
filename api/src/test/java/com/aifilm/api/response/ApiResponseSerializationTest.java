package com.aifilm.api.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ApiResponseTestController.class)
class ApiResponseSerializationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("성공 응답은 status, code, message, data 순서로 직렬화된다")
	void successResponseFieldOrder() throws Exception {
		String body = mockMvc.perform(get("/test/api-response/success"))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString(StandardCharsets.UTF_8);

		assertThat(body).isEqualTo("""
			{"status":200,"code":"SUCCESS","message":"요청에 성공했습니다.","data":"aifilm"}""");
	}

	@Test
	@DisplayName("실패 응답도 같은 순서로 직렬화되고, data는 빠지지 않고 null로 나가며 errors는 없다")
	void failResponseFieldOrder() throws Exception {
		String body = mockMvc.perform(get("/test/api-response/fail"))
			.andExpect(status().isNotFound())
			.andReturn()
			.getResponse()
			.getContentAsString(StandardCharsets.UTF_8);

		assertThat(body).isEqualTo("""
			{"status":404,"code":"TEST_404_1","message":"테스트 리소스를 찾을 수 없습니다.","data":null}""");
	}

	@Test
	@DisplayName("검증 실패 응답은 data 다음에 errors가 직렬화된다")
	void failWithErrorsResponseFieldOrder() throws Exception {
		String body = mockMvc.perform(get("/test/api-response/fail-with-errors"))
			.andExpect(status().isBadRequest())
			.andReturn()
			.getResponse()
			.getContentAsString(StandardCharsets.UTF_8);

		assertThat(body).isEqualTo("""
			{"status":400,"code":"TEST_400_1","message":"테스트 입력값이 올바르지 않습니다.","data":null,"errors":[{"field":"name","reason":"필수 값입니다."}]}""");
	}

}
