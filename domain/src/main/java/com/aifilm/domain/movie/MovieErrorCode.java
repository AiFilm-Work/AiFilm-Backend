package com.aifilm.domain.movie;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import com.aifilm.common.error.ErrorReason;
import com.aifilm.common.error.HttpStatusCodes;

// 영화 도메인의 실패 사유. 없는 영화와 지금 볼 수 없는 영화를 코드로 나눠 프론트가 안내 문구를 다르게 보여줄 수 있게 한다.
@Getter
@RequiredArgsConstructor
public enum MovieErrorCode implements ErrorReason {

	MOVIE_NOT_FOUND(HttpStatusCodes.NOT_FOUND, "MOVIE_404_1", "영화를 찾을 수 없습니다."),
	// 숨김 처리됐거나 YouTube에서 재생할 수 없게 된 영화
	MOVIE_UNAVAILABLE(HttpStatusCodes.NOT_FOUND, "MOVIE_404_2", "지금은 볼 수 없는 영화입니다.");

	private final int status;
	private final String code;
	private final String message;

}
