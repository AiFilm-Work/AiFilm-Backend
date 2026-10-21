package com.aifilm.infra.support;

import java.time.LocalDateTime;
import java.util.Set;

import com.aifilm.domain.movie.Movie;

// 리포지토리 테스트에서 영화를 만드는 헬퍼. 테스트마다 다른 값은 영상 ID, 승인 시각, 장르뿐이다.
public final class MovieFixture {

	private MovieFixture() {
	}

	public static Movie movie(String youtubeVideoId, LocalDateTime approvedAt, Long... genreIds) {
		return Movie.builder()
			.youtubeVideoId(youtubeVideoId)
			.youtubeChannelId("channel-1")
			.youtubeChannelTitle("AI 스튜디오")
			.thumbnailUrl("https://example.com/" + youtubeVideoId + ".jpg")
			.title("영화 " + youtubeVideoId)
			.shortDescription("짧은 소개")
			.runningTimeSeconds(600)
			.approvedAt(approvedAt)
			.genreIds(Set.of(genreIds))
			.build();
	}

}
