package com.aifilm.api.home.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 홈 화면의 영화 섹션 종류. 응답에는 이 순서대로 내려간다.
@Getter
@RequiredArgsConstructor
public enum HomeSectionType {

	LATEST("최신 등록작", false),
	RATING_TOP("평점 TOP 10", true);

	// 화면에 보여줄 섹션 제목
	private final String title;
	// 순위를 보여주는 섹션이면 영화마다 1부터 rankPosition을 채운다
	private final boolean ranked;

}
