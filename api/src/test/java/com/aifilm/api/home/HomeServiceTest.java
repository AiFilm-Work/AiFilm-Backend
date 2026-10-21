package com.aifilm.api.home;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.LongStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.aifilm.api.home.dto.GenreResponse;
import com.aifilm.api.home.dto.HomeBannerResponse;
import com.aifilm.api.home.dto.HomeMovieResponse;
import com.aifilm.api.home.dto.HomeResponse;
import com.aifilm.api.home.dto.HomeSectionResponse;
import com.aifilm.api.home.dto.HomeSectionType;
import com.aifilm.domain.genre.Genre;
import com.aifilm.domain.genre.GenreRepository;
import com.aifilm.domain.genre.MovieGenre;
import com.aifilm.domain.home.HomeBannerRepository;
import com.aifilm.domain.home.VisibleHomeBanner;
import com.aifilm.domain.movie.MovieCard;
import com.aifilm.domain.movie.MovieRepository;

@ExtendWith(MockitoExtension.class)
class HomeServiceTest {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 21, 12, 0);

	@Mock
	private HomeBannerRepository homeBannerRepository;

	@Mock
	private GenreRepository genreRepository;

	@Mock
	private MovieRepository movieRepository;

	private HomeService homeService;

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(NOW.atZone(SEOUL).toInstant(), SEOUL);
		homeService = new HomeService(homeBannerRepository, genreRepository, movieRepository, clock);
	}

	@Test
	@DisplayName("섹션은 10편까지만 내려주고, 한 편 더 조회된 영화가 있으면 hasMore를 true로 내린다")
	void limitsSectionsToTenMovies() {
		given(movieRepository.findLatestScreenable(11)).willReturn(cards(1, 11));
		given(movieRepository.findTopRatedScreenable(11)).willReturn(cards(1, 10));

		HomeResponse home = homeService.getHome();

		HomeSectionResponse latest = home.sections().get(0);
		assertThat(latest.hasMore()).isTrue();
		assertThat(latest.movies()).extracting(HomeMovieResponse::movieId)
			.containsExactlyElementsOf(LongStream.rangeClosed(1, 10).boxed().toList());
		HomeSectionResponse ratingTop = home.sections().get(1);
		assertThat(ratingTop.hasMore()).isFalse();
		assertThat(ratingTop.movies()).hasSize(10);
	}

	@Test
	@DisplayName("섹션은 최신 등록작, 평점 TOP 순서로 내려가고 평점 TOP만 1부터 순위를 채운다")
	void ranksOnlyRatingTopSection() {
		given(movieRepository.findLatestScreenable(11)).willReturn(cards(1, 3));
		given(movieRepository.findTopRatedScreenable(11)).willReturn(cards(4, 6));

		HomeResponse home = homeService.getHome();

		assertThat(home.sections())
			.extracting(HomeSectionResponse::sectionType, HomeSectionResponse::title, HomeSectionResponse::genreId)
			.containsExactly(
				tuple(HomeSectionType.LATEST, "최신 등록작", null),
				tuple(HomeSectionType.RATING_TOP, "평점 TOP 10", null));
		assertThat(home.sections().get(0).movies()).extracting(HomeMovieResponse::rankPosition).containsOnlyNulls();
		assertThat(home.sections().get(1).movies()).extracting(HomeMovieResponse::rankPosition)
			.containsExactly(1, 2, 3);
	}

	@Test
	@DisplayName("Clock의 현재 시각으로 노출 중인 배너를 조회하고, 배너 영화들의 장르를 한 번에 조회해 붙인다")
	void attachesGenresToBanners() {
		given(homeBannerRepository.findVisible(NOW)).willReturn(List.of(banner(10L, 1L), banner(11L, 2L)));
		given(genreRepository.findByMovieIds(List.of(1L, 2L))).willReturn(List.of(
			new MovieGenre(1L, 1L, "SF"),
			new MovieGenre(1L, 3L, "드라마")));

		HomeResponse home = homeService.getHome();

		assertThat(home.banners()).extracting(HomeBannerResponse::bannerId).containsExactly(10L, 11L);
		assertThat(home.banners().get(0).genres())
			.containsExactly(new GenreResponse(1L, "SF"), new GenreResponse(3L, "드라마"));
		assertThat(home.banners().get(1).genres()).isEmpty();
	}

	@Test
	@DisplayName("장르 메뉴는 장르 ID 순서 그대로 내려간다")
	void returnsGenreMenu() {
		given(genreRepository.findAllOrderById()).willReturn(List.of(genre(1L, "SF"), genre(2L, "공포")));

		HomeResponse home = homeService.getHome();

		assertThat(home.genres()).containsExactly(new GenreResponse(1L, "SF"), new GenreResponse(2L, "공포"));
	}

	private static List<MovieCard> cards(long fromId, long toId) {
		return LongStream.rangeClosed(fromId, toId)
			.mapToObj(id -> new MovieCard(id, "영화 " + id, "https://example.com/" + id + ".jpg",
				new BigDecimal("4.50"), 10))
			.toList();
	}

	private static VisibleHomeBanner banner(Long bannerId, Long movieId) {
		return new VisibleHomeBanner(bannerId, movieId, "영화 " + movieId, "짧은 소개",
			"https://example.com/" + movieId + ".jpg", "video-" + movieId, new BigDecimal("4.50"), 10, 0, 10);
	}

	// 장르 ID는 DB가 채우는 값이라 테스트에서는 직접 넣는다
	private static Genre genre(Long id, String name) {
		Genre genre = new Genre(name, null);
		ReflectionTestUtils.setField(genre, "id", id);
		return genre;
	}

}
