package com.aifilm.api.docs;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.aifilm.api.response.ApiResponse;
import com.aifilm.common.error.BusinessException;
import com.aifilm.common.error.ErrorReason;
import com.aifilm.common.error.HttpStatusCodes;

// API 문서화 파이프라인 확인용 테스트 전용 샘플 API. 첫 실제 API를 문서화할 때 삭제한다.
@RestController
class DocsSampleController {

	private static final long EXISTING_SAMPLE_ID = 1L;

	@GetMapping("/api/v1/docs-samples/{sampleId}")
	public ApiResponse<SampleResponse> getSample(@PathVariable long sampleId) {
		if (sampleId != EXISTING_SAMPLE_ID) {
			throw new BusinessException(SampleErrorCode.NOT_FOUND);
		}
		return ApiResponse.success(new SampleResponse(sampleId, "문서화 샘플"));
	}

	record SampleResponse(long sampleId, String name) {
	}

	@Getter
	@RequiredArgsConstructor
	enum SampleErrorCode implements ErrorReason {

		NOT_FOUND(HttpStatusCodes.NOT_FOUND, "SAMPLE_404_1", "샘플을 찾을 수 없습니다.");

		private final int status;
		private final String code;
		private final String message;
	}

}
