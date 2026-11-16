package com.aifilm.domain.home;

import java.math.BigDecimal;

// 지금 홈에 노출되는 배너와 그 영화의 표시 정보를 담은 조회 모델. description은 영화의 짧은 소개다.
// youtubeVideoId는 youtube_movie_source에서 가져온다. YouTube 출처가 없는 영화면 null이다.
public record VisibleHomeBanner(Long bannerId, Long movieId, String title, String description, String thumbnailUrl,
	String youtubeVideoId, BigDecimal averageRating, long reviewCount, int startSeconds, int previewDurationSeconds) {
}
