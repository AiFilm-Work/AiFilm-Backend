package com.aifilm.api.home.dto;

import java.math.BigDecimal;
import java.util.List;

import com.aifilm.api.genre.dto.GenreResponse;
import com.aifilm.domain.home.VisibleHomeBanner;

// 홈 상단 배너 하나. 프론트는 youtubeVideoId의 startSeconds부터 previewDurationSeconds 동안 미리보기를 재생한다.
public record HomeBannerResponse(Long bannerId, Long movieId, String title, String description, String thumbnailUrl,
	String youtubeVideoId, BigDecimal averageRating, long reviewCount, List<GenreResponse> genres, int startSeconds,
	int previewDurationSeconds) {

	public static HomeBannerResponse of(VisibleHomeBanner banner, List<GenreResponse> genres) {
		return new HomeBannerResponse(banner.bannerId(), banner.movieId(), banner.title(), banner.description(),
			banner.thumbnailUrl(), banner.youtubeVideoId(), banner.averageRating(), banner.reviewCount(), genres,
			banner.startSeconds(), banner.previewDurationSeconds());
	}

}
