package com.aifilm.api.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

// 브라우저에서 API를 호출할 수 있는 프론트 출처 목록. 비어 있으면 다른 출처의 브라우저 요청을 허용하지 않는다.
@ConfigurationProperties("aifilm.cors")
public record CorsProperties(List<String> allowedOrigins) {

	public CorsProperties {
		// 설정이 없거나 환경변수가 빈 문자열로 들어와도 빈 출처가 허용 목록에 남지 않게 한다
		allowedOrigins = allowedOrigins == null ? List.of() : allowedOrigins.stream()
			.map(String::trim)
			.filter(origin -> !origin.isEmpty())
			.toList();
	}

}
