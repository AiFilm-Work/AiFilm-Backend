package com.aifilm.api.response;

// 응답의 날짜·시각 형식. 서버 시간대(KST)의 LocalDateTime을 오프셋 없이 초까지 내린다.
// DB는 마이크로초까지 저장해서 형식을 정하지 않으면 값마다 소수점 아래 자릿수가 달라진다.
// 응답 DTO의 LocalDateTime 필드에 @JsonFormat(pattern = ApiDateTimeFormat.PATTERN)으로 붙인다.
public final class ApiDateTimeFormat {

	public static final String PATTERN = "yyyy-MM-dd'T'HH:mm:ss";

	private ApiDateTimeFormat() {
	}

}
