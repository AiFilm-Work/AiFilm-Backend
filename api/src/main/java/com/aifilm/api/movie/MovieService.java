package com.aifilm.api.movie;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aifilm.api.genre.dto.GenreResponse;
import com.aifilm.api.movie.dto.MovieBadgeResponse;
import com.aifilm.api.movie.dto.MovieBadgeType;
import com.aifilm.api.movie.dto.MovieDetailResponse;
import com.aifilm.common.error.BusinessException;
import com.aifilm.domain.genre.GenreRepository;
import com.aifilm.domain.movie.MovieDetail;
import com.aifilm.domain.movie.MovieErrorCode;
import com.aifilm.domain.movie.MovieRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovieService {

	private final MovieRepository movieRepository;
	private final GenreRepository genreRepository;
	private final Clock clock;

	// 영화 1번, 장르 1번으로 조회한다. 볼 수 없는 영화는 장르를 조회하지 않고 바로 실패한다.
	public MovieDetailResponse getMovieDetail(Long movieId) {
		MovieDetail detail = movieRepository.findDetailById(movieId)
			.orElseThrow(() -> new BusinessException(MovieErrorCode.MOVIE_NOT_FOUND));
		if (!detail.isScreenable()) {
			throw new BusinessException(MovieErrorCode.MOVIE_UNAVAILABLE);
		}
		List<GenreResponse> genres = genreRepository.findByMovieIds(List.of(movieId)).stream()
			.map(GenreResponse::from)
			.toList();
		return MovieDetailResponse.of(detail, genres, badgesOf(detail));
	}

	// 배지는 저장하지 않고 조회할 때 현재 시각으로 계산한다
	private List<MovieBadgeResponse> badgesOf(MovieDetail detail) {
		if (detail.isNewAt(LocalDateTime.now(clock))) {
			return List.of(MovieBadgeResponse.from(MovieBadgeType.NEW));
		}
		return List.of();
	}

}
