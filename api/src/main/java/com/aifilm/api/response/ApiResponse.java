package com.aifilm.api.response;

import lombok.Getter;

import org.springframework.http.HttpStatus;

import com.aifilm.common.error.ErrorReason;

@Getter
public class ApiResponse<T> {

	// 프론트엔드는 code가 SUCCESS인지만 보고 성공 여부를 판단한다.
	private static final String SUCCESS_CODE = "SUCCESS";
	private static final String SUCCESS_MESSAGE = "요청에 성공했습니다.";

	private final int status;
	private final String code;
	private final String message;
	private final T data;

	private ApiResponse(int status, String code, String message, T data) {
		this.status = status;
		this.code = code;
		this.message = message;
		this.data = data;
	}

	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(HttpStatus.OK.value(), SUCCESS_CODE, SUCCESS_MESSAGE, data);
	}

	public static <T> ApiResponse<T> fail(ErrorReason errorReason) {
		return new ApiResponse<>(errorReason.getStatus(), errorReason.getCode(), errorReason.getMessage(), null);
	}

}
