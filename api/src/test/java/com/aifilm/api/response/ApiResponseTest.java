package com.aifilm.api.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApiResponseTest {

	@Test
	@DisplayName("success는 200, SUCCESS 코드, 성공 메시지와 함께 데이터를 담는다")
	void success() {
		ApiResponse<String> response = ApiResponse.success("data");

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getCode()).isEqualTo("SUCCESS");
		assertThat(response.getMessage()).isEqualTo("요청에 성공했습니다.");
		assertThat(response.getData()).isEqualTo("data");
	}

	@Test
	@DisplayName("fail은 ErrorReason의 상태, 코드, 메시지를 그대로 담고 데이터는 비운다")
	void fail() {
		ApiResponse<Void> response = ApiResponse.fail(TestErrorCode.NOT_FOUND);

		assertThat(response.getStatus()).isEqualTo(404);
		assertThat(response.getCode()).isEqualTo("TEST_404_1");
		assertThat(response.getMessage()).isEqualTo("테스트 리소스를 찾을 수 없습니다.");
		assertThat(response.getData()).isNull();
		assertThat(response.getErrors()).isNull();
	}

	@Test
	@DisplayName("fail에 필드 오류를 넘기면 errors에 담고 데이터는 비운다")
	void failWithErrors() {
		List<FieldErrorResponse> errors = List.of(new FieldErrorResponse("name", "필수 값입니다."));

		ApiResponse<Void> response = ApiResponse.fail(TestErrorCode.INVALID, errors);

		assertThat(response.getStatus()).isEqualTo(400);
		assertThat(response.getCode()).isEqualTo("TEST_400_1");
		assertThat(response.getData()).isNull();
		assertThat(response.getErrors()).containsExactly(new FieldErrorResponse("name", "필수 값입니다."));
	}

}
