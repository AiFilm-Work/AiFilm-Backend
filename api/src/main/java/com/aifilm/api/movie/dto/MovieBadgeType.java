package com.aifilm.api.movie.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 상세 화면 배지 종류와 화면에 보여줄 문구. 순위 배지(TOP 10)는 랭킹 집계가 생기면 추가한다.
@Getter
@RequiredArgsConstructor
public enum MovieBadgeType {

	NEW("New");

	private final String label;

}
