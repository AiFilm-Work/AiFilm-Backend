package com.aifilm.api.home.dto;

import java.util.List;

// 홈 화면 조회 응답. 배너, 장르 메뉴, 영화 섹션을 한 번에 내려준다.
public record HomeResponse(List<HomeBannerResponse> banners, List<GenreResponse> genres,
	List<HomeSectionResponse> sections) {
}
