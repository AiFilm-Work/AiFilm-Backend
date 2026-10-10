package com.aifilm.api.response;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// ApiResponse가 실제 HTTP 응답 본문으로 어떻게 나가는지 확인하기 위한 테스트 전용 컨트롤러
@RestController
class ApiResponseTestController {

	@GetMapping("/test/api-response/success")
	public ApiResponse<String> success() {
		return ApiResponse.success("aifilm");
	}

	@GetMapping("/test/api-response/fail")
	public ResponseEntity<ApiResponse<Void>> fail() {
		return ResponseEntity.status(TestErrorCode.NOT_FOUND.getStatus())
			.body(ApiResponse.fail(TestErrorCode.NOT_FOUND));
	}

	@GetMapping("/test/api-response/fail-with-errors")
	public ResponseEntity<ApiResponse<Void>> failWithErrors() {
		return ResponseEntity.status(TestErrorCode.INVALID.getStatus())
			.body(ApiResponse.fail(TestErrorCode.INVALID, List.of(new FieldErrorResponse("name", "필수 값입니다."))));
	}

}
