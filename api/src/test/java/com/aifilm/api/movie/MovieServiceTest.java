package com.aifilm.api.movie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aifilm.api.genre.dto.GenreResponse;
import com.aifilm.api.movie.dto.MovieBadgeResponse;
import com.aifilm.api.movie.dto.MovieBadgeType;
import com.aifilm.api.movie.dto.MovieDetailResponse;
import com.aifilm.api.movie.dto.MovieStatisticsResponse;
import com.aifilm.common.error.BusinessException;
import com.aifilm.domain.genre.GenreRepository;
import com.aifilm.domain.genre.MovieGenre;
import com.aifilm.domain.movie.MovieDetail;
import com.aifilm.domain.movie.MovieErrorCode;
import com.aifilm.domain.movie.MovieRepository;
import com.aifilm.domain.movie.ScreeningStatus;
import com.aifilm.domain.movie.YoutubeAvailabilityStatus;

@ExtendWith(MockitoExtension.class)
class MovieServiceTest {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 26, 12, 0);
	private static final Long MOVIE_ID = 101L;

	@Mock
	private MovieRepository movieRepository;

	@Mock
	private GenreRepository genreRepository;

	private MovieService movieService;

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(NOW.atZone(SEOUL).toInstant(), SEOUL);
		movieService = new MovieService(movieRepository, genreRepository, clock);
	}

	@Test
	@DisplayName("상세 조회는 영화 정보에 장르와 통계를 붙여 응답한다")
	void getsMovieDetail() {
		given(movieRepository.findDetailById(MOVIE_ID)).willReturn(Optional.of(
			detail(NOW.minusDays(30), ScreeningStatus.ACTIVE, YoutubeAvailabilityStatus.AVAILABLE)));
		given(genreRepository.findByMovieIds(List.of(MOVIE_ID))).willReturn(List.of(
			new MovieGenre(MOVIE_ID, 1L, "SF"),
			new MovieGenre(MOVIE_ID, 5L, "로맨스")));

		MovieDetailResponse response = movieService.getMovieDetail(MOVIE_ID);

		assertThat(response.movieId()).isEqualTo(MOVIE_ID);
		assertThat(response.youtubeVideoId()).isEqualTo("abc123");
		assertThat(response.genres()).containsExactly(new GenreResponse(1L, "SF"), new GenreResponse(5L, "로맨스"));
		assertThat(response.statistics()).isEqualTo(new MovieStatisticsResponse(new BigDecimal("4.70"), 21, 30));
		assertThat(response.badges()).isEmpty();
	}

	@Test
	@DisplayName("승인 후 7일이 지나기 전인 영화에는 NEW 배지를 붙인다")
	void addsNewBadgeWithinSevenDays() {
		given(movieRepository.findDetailById(MOVIE_ID)).willReturn(Optional.of(
			detail(NOW.minusDays(6), ScreeningStatus.ACTIVE, YoutubeAvailabilityStatus.AVAILABLE)));

		MovieDetailResponse response = movieService.getMovieDetail(MOVIE_ID);

		assertThat(response.badges()).containsExactly(new MovieBadgeResponse(MovieBadgeType.NEW, "New", null));
	}

	@Test
	@DisplayName("없는 영화면 MOVIE_NOT_FOUND로 실패하고 장르를 조회하지 않는다")
	void failsWhenMovieMissing() {
		given(movieRepository.findDetailById(MOVIE_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> movieService.getMovieDetail(MOVIE_ID))
			.isInstanceOfSatisfying(BusinessException.class,
				e -> assertThat(e.getErrorReason()).isEqualTo(MovieErrorCode.MOVIE_NOT_FOUND));
		then(genreRepository).shouldHaveNoInteractions();
	}

	@ParameterizedTest
	@CsvSource({"HIDDEN, AVAILABLE", "UNAVAILABLE, AVAILABLE", "ACTIVE, UNAVAILABLE"})
	@DisplayName("숨김 처리됐거나 YouTube에서 재생할 수 없는 영화면 MOVIE_UNAVAILABLE로 실패하고 장르를 조회하지 않는다")
	void failsWhenMovieNotScreenable(ScreeningStatus screeningStatus, YoutubeAvailabilityStatus availabilityStatus) {
		given(movieRepository.findDetailById(MOVIE_ID)).willReturn(Optional.of(
			detail(NOW.minusDays(30), screeningStatus, availabilityStatus)));

		assertThatThrownBy(() -> movieService.getMovieDetail(MOVIE_ID))
			.isInstanceOfSatisfying(BusinessException.class,
				e -> assertThat(e.getErrorReason()).isEqualTo(MovieErrorCode.MOVIE_UNAVAILABLE));
		then(genreRepository).shouldHaveNoInteractions();
	}

	private static MovieDetail detail(LocalDateTime approvedAt, ScreeningStatus screeningStatus,
		YoutubeAvailabilityStatus availabilityStatus) {
		return new MovieDetail(MOVIE_ID, "네온 메모리", "기억은 사라져도, 사랑은 남는다.", "YouTube 원본 설명",
			"https://example.com/neon-memory.jpg", "abc123", "AI Film Studio", 766,
			LocalDateTime.of(2025, 1, 10, 13, 20), approvedAt, new BigDecimal("4.70"), 21, 30,
			screeningStatus, availabilityStatus);
	}

}
