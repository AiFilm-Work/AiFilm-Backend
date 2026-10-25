package com.aifilm.api.movie.dto;

// 상세 화면 배지 하나. rankPosition은 순위 배지에서만 채우고, NEW 같은 배지는 null이다.
public record MovieBadgeResponse(MovieBadgeType type, String label, Integer rankPosition) {

	public static MovieBadgeResponse from(MovieBadgeType type) {
		return new MovieBadgeResponse(type, type.getLabel(), null);
	}

}
