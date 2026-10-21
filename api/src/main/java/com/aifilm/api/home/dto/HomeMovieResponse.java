package com.aifilm.api.home.dto;

import java.math.BigDecimal;

import com.aifilm.domain.movie.MovieCard;

// 섹션 안의 영화 카드. rankPosition은 순위 섹션에서만 1부터 채우고, 그 외 섹션은 null이다.
public record HomeMovieResponse(Long movieId, String title, String thumbnailUrl, BigDecimal averageRating,
	long reviewCount, Integer rankPosition) {

	public static HomeMovieResponse of(MovieCard card, Integer rankPosition) {
		return new HomeMovieResponse(card.movieId(), card.title(), card.thumbnailUrl(), card.averageRating(),
			card.reviewCount(), rankPosition);
	}

}
