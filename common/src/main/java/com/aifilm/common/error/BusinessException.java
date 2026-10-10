package com.aifilm.common.error;

import lombok.Getter;

// 서비스/도메인에서 실패를 알릴 때 던진다. HTTP 응답 변환은 api의 전역 핸들러가 맡는다.
@Getter
public class BusinessException extends RuntimeException {

	private final ErrorReason errorReason;

	public BusinessException(ErrorReason errorReason) {
		super(errorReason.getMessage());
		this.errorReason = errorReason;
	}

	// 외부 API, DB 예외 등을 감쌀 때 원인 예외를 로그에 남기기 위해 받는다.
	public BusinessException(ErrorReason errorReason, Throwable cause) {
		super(errorReason.getMessage(), cause);
		this.errorReason = errorReason;
	}

}
