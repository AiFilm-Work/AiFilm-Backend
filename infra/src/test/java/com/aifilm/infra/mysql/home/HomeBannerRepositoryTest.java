package com.aifilm.infra.mysql.home;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.aifilm.domain.home.HomeBanner;
import com.aifilm.domain.home.HomeBannerRepository;
import com.aifilm.domain.home.VisibleHomeBanner;
import com.aifilm.domain.movie.ScreeningStatus;
import com.aifilm.domain.movie.YoutubeAvailabilityStatus;
import com.aifilm.infra.support.MovieFixture;
import com.aifilm.infra.support.RepositoryTest;

@RepositoryTest
@Import(HomeBannerRepositoryImpl.class)
class HomeBannerRepositoryTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 21, 12, 0);

	@Autowired
	private HomeBannerRepository homeBannerRepository;

	@Autowired
	private TestEntityManager entityManager;

	private Long movieId;

	@BeforeEach
	void setUp() {
		movieId = saveMovie("banner-movie");
	}

	@Test
	@DisplayName("배너를 저장하면 ID와 생성 시각이 채워지고 기본 미리보기 구간으로 저장된다")
	void savesBannerWithDefaults() {
		HomeBanner saved = homeBannerRepository.save(HomeBanner.builder().movieId(movieId).displayOrder(1).build());
		// 1차 캐시가 아니라 DB에서 다시 읽어 오도록 비운다
		entityManager.flush();
		entityManager.clear();

		HomeBanner found = entityManager.find(HomeBanner.class, saved.getId());

		assertThat(found.getMovieId()).isEqualTo(movieId);
		assertThat(found.getDisplayOrder()).isEqualTo(1);
		assertThat(found.getStartSeconds()).isZero();
		assertThat(found.getPreviewDurationSeconds()).isEqualTo(10);
		assertThat(found.isActive()).isTrue();
		assertThat(found.getCreatedAt()).isNotNull();
		assertThat(found.getUpdatedAt()).isNotNull();
	}

	@Test
	@DisplayName("같은 영화로 배너를 두 개 만들 수 없다")
	void rejectsSecondBannerForSameMovie() {
		homeBannerRepository.save(HomeBanner.builder().movieId(movieId).displayOrder(1).build());
		entityManager.flush();

		assertThatThrownBy(() -> {
			homeBannerRepository.save(HomeBanner.builder().movieId(movieId).displayOrder(2).build());
			entityManager.flush();
		}).hasRootCauseInstanceOf(SQLIntegrityConstraintViolationException.class);
	}

	@Test
	@DisplayName("없는 영화로는 배너를 만들 수 없다")
	void rejectsUnknownMovie() {
		assertThatThrownBy(() -> {
			homeBannerRepository.save(HomeBanner.builder().movieId(999_999L).displayOrder(1).build());
			entityManager.flush();
		}).hasRootCauseInstanceOf(SQLIntegrityConstraintViolationException.class);
	}

	// 관리자 API가 생기기 전에는 운영 배너를 SQL로 넣는다. 엔티티 검증을 거치지 않아도 DB가 막는지 확인한다.
	@ParameterizedTest(name = "{5}")
	@CsvSource({
		"0, 0, 10, 2026-10-01T00:00, 2026-10-31T00:00, ck_home_banner_movie_display_order",
		"1, -1, 10, 2026-10-01T00:00, 2026-10-31T00:00, ck_home_banner_movie_start_seconds",
		"1, 0, 0, 2026-10-01T00:00, 2026-10-31T00:00, ck_home_banner_movie_preview_duration_seconds",
		"1, 0, 10, 2026-10-31T00:00, 2026-10-31T00:00, ck_home_banner_movie_period"
	})
	@DisplayName("SQL로 직접 넣어도 잘못된 노출 순서·미리보기 구간·노출 기간은 DB CHECK에 막힌다")
	void rejectsInvalidBannerInsertedBySql(int displayOrder, int startSeconds, int previewDurationSeconds,
		LocalDateTime startsAt, LocalDateTime endsAt, String constraintName) {
		assertThatThrownBy(() -> entityManager.getEntityManager()
			.createNativeQuery("""
				INSERT INTO home_banner_movie
				    (movie_id, display_order, start_seconds, preview_duration_seconds, starts_at, ends_at)
				VALUES (?, ?, ?, ?, ?, ?)
				""")
			.setParameter(1, movieId)
			.setParameter(2, displayOrder)
			.setParameter(3, startSeconds)
			.setParameter(4, previewDurationSeconds)
			.setParameter(5, startsAt)
			.setParameter(6, endsAt)
			.executeUpdate())
			.rootCause()
			.hasMessageContaining(constraintName);
	}

	@Test
	@DisplayName("노출 중인 배너만 노출 순서대로, 순서가 같으면 먼저 만든 배너부터 조회한다")
	void findsVisibleBannersInDisplayOrder() {
		Long second = saveBanner(banner(saveMovie("order-2"), 2));
		// 노출 시작 시각은 포함한다
		Long first = saveBanner(banner(saveMovie("order-1"), 1).startsAt(NOW).endsAt(NOW.plusDays(1)));
		Long sameOrderLater = saveBanner(banner(saveMovie("order-2-later"), 2).endsAt(NOW.plusSeconds(1)));
		saveBanner(banner(saveMovie("inactive"), 1).active(false));
		saveBanner(banner(saveMovie("not-started"), 1).startsAt(NOW.plusSeconds(1)));
		// 노출 종료 시각은 포함하지 않는다
		saveBanner(banner(saveMovie("ended"), 1).endsAt(NOW));
		Long hiddenMovie = saveMovie("hidden-movie");
		saveBanner(banner(hiddenMovie, 1));
		MovieFixture.changeStatus(entityManager, hiddenMovie, ScreeningStatus.HIDDEN,
			YoutubeAvailabilityStatus.AVAILABLE);
		Long unavailableMovie = saveMovie("unavailable-movie");
		saveBanner(banner(unavailableMovie, 1));
		MovieFixture.changeStatus(entityManager, unavailableMovie, ScreeningStatus.ACTIVE,
			YoutubeAvailabilityStatus.UNAVAILABLE);

		assertThat(homeBannerRepository.findVisible(NOW))
			.extracting(VisibleHomeBanner::bannerId)
			.containsExactly(first, second, sameOrderLater);
	}

	@Test
	@DisplayName("배너에는 영화의 제목, 짧은 소개, 썸네일, 영상 ID와 미리보기 구간이 담긴다")
	void mapsBannerWithMovie() {
		saveBanner(banner(movieId, 1).startSeconds(20).previewDurationSeconds(15));

		VisibleHomeBanner banner = homeBannerRepository.findVisible(NOW).getFirst();

		assertThat(banner.movieId()).isEqualTo(movieId);
		assertThat(banner.title()).isEqualTo("영화 banner-movie");
		assertThat(banner.description()).isEqualTo("짧은 소개");
		assertThat(banner.thumbnailUrl()).isEqualTo("https://example.com/banner-movie.jpg");
		assertThat(banner.youtubeVideoId()).isEqualTo("banner-movie");
		assertThat(banner.averageRating()).isEqualByComparingTo("0");
		assertThat(banner.reviewCount()).isZero();
		assertThat(banner.startSeconds()).isEqualTo(20);
		assertThat(banner.previewDurationSeconds()).isEqualTo(15);
	}

	private Long saveMovie(String youtubeVideoId) {
		return entityManager.persistAndFlush(
			MovieFixture.movie(youtubeVideoId, LocalDateTime.of(2026, 10, 20, 12, 0), 1L)).getId();
	}

	private static HomeBanner.HomeBannerBuilder banner(Long movieId, int displayOrder) {
		return HomeBanner.builder().movieId(movieId).displayOrder(displayOrder);
	}

	private Long saveBanner(HomeBanner.HomeBannerBuilder banner) {
		return homeBannerRepository.save(banner.build()).getId();
	}

}
