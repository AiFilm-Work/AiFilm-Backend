package com.aifilm.api.exception;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.aifilm.common.error.BusinessException;

// 전역 예외 핸들러가 예외 종류별로 어떤 응답을 내리는지 확인하기 위한 테스트 전용 컨트롤러
@RestController
class ExceptionTestController {

	@GetMapping("/test/exceptions/business")
	public void business() {
		throw new BusinessException(ExceptionTestErrorCode.CONFLICT);
	}

	@GetMapping("/test/exceptions/unexpected")
	public void unexpected() {
		throw new IllegalStateException("내부 구현 정보가 담긴 메시지");
	}

	@PostMapping(value = "/test/exceptions/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public void json(@RequestBody Map<String, Object> body) {
	}

}
