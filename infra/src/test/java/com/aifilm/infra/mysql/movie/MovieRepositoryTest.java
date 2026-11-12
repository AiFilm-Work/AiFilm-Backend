package com.aifilm.infra.mysql.movie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.aifilm.domain.movie.AvailabilityStatus;
import com.aifilm.domain.movie.Movie;
import com.aifilm.domain.movie.MovieCard;
import com.aifilm.domain.movie.MovieDetail;
import com.aifilm.domain.movie.MovieRepository;
import com.aifilm.domain.movie.ScreeningStatus;
import com.aifilm.infra.support.MovieFixture;
import com.aifilm.infra.support.RepositoryTest;

@RepositoryTest
@Import(MovieRepositoryImpl.class)
class MovieRepositoryTest {

	@Autowired
	private MovieRepository movieRepository;

	@Autowired
	private TestEntityManager entityManager;

	private static Movie movie(String youtubeVideoId, Set<Long> genreIds) {
		return Movie.builder()
			.youtubeVideoId(youtubeVideoId)
			.youtubeChannelId("channel-1")
			.youtubeChannelTitle("AI 스튜디오")
			.thumbnailUrl("https://example.com/neon-memory.jpg")
			.title("네온 메모리")
			.youtubeDescription("기억과 사랑을 주제로 제작한 AI 단편영화입니다.")
			.shortDescription("기억은 사라져도, 사랑은 남는다.")
			.runningTimeSeconds(766)
			.youtubePublishedAt(LocalDateTime.of(2025, 1, 10, 13, 20))
			.approvedAt(LocalDateTime.of(2026, 10, 20, 12, 0))
			.genreIds(genreIds)
			.build();
	}

	@Test
	@DisplayName("영화를 장르와 함께 저장하고 다시 조회한다")
	void savesAndFindsMovieWithGenres() {
		Movie saved = movieRepository.save(movie("abc123", Set.of(1L, 3L)));
		// 1차 캐시가 아니라 DB에서 다시 읽어 오도록 비운다
		entityManager.flush();
		entityManager.clear();

		Movie found = movieRepository.findById(saved.getId()).orElseThrow();

		assertThat(found.getYoutubeVideoId()).isEqualTo("abc123");
		assertThat(found.getRunningTimeSeconds()).isEqualTo(766);
		assertThat(found.getScreeningStatus()).isEqualTo(ScreeningStatus.ACTIVE);
		assertThat(found.getGenreIds()).containsExactlyInAnyOrder(1L, 3L);
		assertThat(found.getCreatedAt()).isNotNull();
		assertThat(found.getUpdatedAt()).isNotNull();
	}

	@Test
	@DisplayName("같은 YouTube 영상은 두 번 저장되지 않는다")
	void rejectsDuplicateYoutubeVideoId() {
		movieRepository.save(movie("dup-video", Set.of(1L)));
		entityManager.flush();

		assertThatThrownBy(() -> {
			movieRepository.save(movie("dup-video", Set.of(2L)));
			entityManager.flush();
		}).hasRootCauseInstanceOf(SQLIntegrityConstraintViolationException.class);
	}

	@Test
	@DisplayName("없는 장르 ID로는 영화를 저장할 수 없다")
	void rejectsUnknownGenreId() {
		assertThatThrownBy(() -> {
			movieRepository.save(movie("unknown-genre", Set.of(999L)));
			entityManager.flush();
		}).hasRootCauseInstanceOf(SQLIntegrityConstraintViolationException.class);
	}

	@Test
	@DisplayName("최신 등록작은 노출 중이고 재생 가능한 영화만 승인 시각이 늦은 순서로 limit개까지 조회한다")
	void findsLatestScreenableMovies() {
		Long oldest = save("latest-1", day(1));
		Long middle = save("latest-2", day(2));
		Long newest = save("latest-3", day(3));
		Long hidden = save("latest-hidden", day(4));
		Long unavailable = save("latest-unavailable", day(5));
		MovieFixture.changeStatus(entityManager, hidden, ScreeningStatus.HIDDEN, AvailabilityStatus.AVAILABLE);
		MovieFixture.changeStatus(entityManager, unavailable,
			ScreeningStatus.ACTIVE, AvailabilityStatus.UNAVAILABLE);

		assertThat(movieRepository.findLatestScreenable(10))
			.extracting(MovieCard::movieId)
			.containsExactly(newest, middle, oldest);
		assertThat(movieRepository.findLatestScreenable(2))
			.extracting(MovieCard::movieId)
			.containsExactly(newest, middle);
	}

	@Test
	@DisplayName("승인 시각이 같으면 나중에 등록한 영화가 먼저 나온다")
	void ordersSameApprovedAtByLatestId() {
		Long first = save("same-time-1", day(1));
		Long second = save("same-time-2", day(1));

		assertThat(movieRepository.findLatestScreenable(10))
			.extracting(MovieCard::movieId)
			.containsExactly(second, first);
	}

	@Test
	@DisplayName("카드에는 목록에 보여줄 제목, 썸네일, 평점, 리뷰 수가 담긴다")
	void mapsCardColumns() {
		Long movieId = save("card", day(1));
		MovieFixture.changeRating(entityManager, movieId, "4.50", 12);

		MovieCard card = movieRepository.findLatestScreenable(1).getFirst();

		assertThat(card.movieId()).isEqualTo(movieId);
		assertThat(card.title()).isEqualTo("영화 card");
		assertThat(card.thumbnailUrl()).isEqualTo("https://example.com/card.jpg");
		assertThat(card.averageRating()).isEqualByComparingTo("4.50");
		assertThat(card.reviewCount()).isEqualTo(12);
	}

	@Test
	@DisplayName("평점 TOP은 평점이 높은 순서로, 같으면 리뷰 수, 그것도 같으면 승인 시각이 늦은 순서로 조회한다")
	void findsTopRatedScreenableMovies() {
		Long best = saveRated("rating-best", day(1), "4.80", 10);
		Long moreReviews = saveRated("rating-more-reviews", day(1), "4.50", 30);
		Long newer = saveRated("rating-newer", day(3), "4.50", 10);
		Long older = saveRated("rating-older", day(2), "4.50", 10);
		Long lowest = saveRated("rating-lowest", day(5), "3.00", 100);
		Long hidden = saveRated("rating-hidden", day(1), "5.00", 100);
		Long unavailable = saveRated("rating-unavailable", day(1), "5.00", 100);
		MovieFixture.changeStatus(entityManager, hidden, ScreeningStatus.HIDDEN, AvailabilityStatus.AVAILABLE);
		MovieFixture.changeStatus(entityManager, unavailable,
			ScreeningStatus.ACTIVE, AvailabilityStatus.UNAVAILABLE);

		assertThat(movieRepository.findTopRatedScreenable(10))
			.extracting(MovieCard::movieId)
			.containsExactly(best, moreReviews, newer, older, lowest);
		assertThat(movieRepository.findTopRatedScreenable(3))
			.extracting(MovieCard::movieId)
			.containsExactly(best, moreReviews, newer);
	}

	@Test
	@DisplayName("상세 조회는 상세 화면에 보여줄 값과 노출 상태를 담는다")
	void findsDetail() {
		Movie saved = movieRepository.save(movie("detail", Set.of(1L)));
		entityManager.flush();
		MovieFixture.changeRating(entityManager, saved.getId(), "4.70", 21);

		MovieDetail detail = movieRepository.findDetailById(saved.getId()).orElseThrow();

		assertThat(detail.movieId()).isEqualTo(saved.getId());
		assertThat(detail.title()).isEqualTo("네온 메모리");
		assertThat(detail.shortDescription()).isEqualTo("기억은 사라져도, 사랑은 남는다.");
		assertThat(detail.youtubeDescription()).isEqualTo("기억과 사랑을 주제로 제작한 AI 단편영화입니다.");
		assertThat(detail.thumbnailUrl()).isEqualTo("https://example.com/neon-memory.jpg");
		assertThat(detail.youtubeVideoId()).isEqualTo("detail");
		assertThat(detail.youtubeChannelTitle()).isEqualTo("AI 스튜디오");
		assertThat(detail.runningTimeSeconds()).isEqualTo(766);
		assertThat(detail.youtubePublishedAt()).isEqualTo(LocalDateTime.of(2025, 1, 10, 13, 20));
		assertThat(detail.approvedAt()).isEqualTo(LocalDateTime.of(2026, 10, 20, 12, 0));
		assertThat(detail.averageRating()).isEqualByComparingTo("4.70");
		assertThat(detail.reviewCount()).isEqualTo(21);
		assertThat(detail.totalViewCount()).isZero();
		assertThat(detail.screeningStatus()).isEqualTo(ScreeningStatus.ACTIVE);
		assertThat(detail.availabilityStatus()).isEqualTo(AvailabilityStatus.AVAILABLE);
	}

	@Test
	@DisplayName("상세 조회는 숨기거나 재생할 수 없는 영화도 상태와 함께 조회한다")
	void findsDetailRegardlessOfStatus() {
		Long hidden = save("detail-hidden", day(1));
		Long unavailable = save("detail-unavailable", day(1));
		MovieFixture.changeStatus(entityManager, hidden, ScreeningStatus.HIDDEN, AvailabilityStatus.AVAILABLE);
		MovieFixture.changeStatus(entityManager, unavailable,
			ScreeningStatus.ACTIVE, AvailabilityStatus.UNAVAILABLE);

		assertThat(movieRepository.findDetailById(hidden).orElseThrow().screeningStatus())
			.isEqualTo(ScreeningStatus.HIDDEN);
		assertThat(movieRepository.findDetailById(unavailable).orElseThrow().availabilityStatus())
			.isEqualTo(AvailabilityStatus.UNAVAILABLE);
	}

	@Test
	@DisplayName("YouTube 설명과 게시일이 없는 영화는 두 값이 null로 조회된다")
	void findsDetailWithoutYoutubeDescription() {
		Long movieId = save("detail-no-description", day(1));

		MovieDetail detail = movieRepository.findDetailById(movieId).orElseThrow();

		assertThat(detail.youtubeDescription()).isNull();
		assertThat(detail.youtubePublishedAt()).isNull();
	}

	@Test
	@DisplayName("없는 영화는 빈 값으로 조회된다")
	void findsNoDetailForMissingMovie() {
		assertThat(movieRepository.findDetailById(Long.MAX_VALUE)).isEmpty();
	}

	private Long save(String youtubeVideoId, LocalDateTime approvedAt) {
		return entityManager.persistAndFlush(MovieFixture.movie(youtubeVideoId, approvedAt, 1L)).getId();
	}

	private Long saveRated(String youtubeVideoId, LocalDateTime approvedAt, String averageRating, long reviewCount) {
		Long movieId = save(youtubeVideoId, approvedAt);
		MovieFixture.changeRating(entityManager, movieId, averageRating, reviewCount);
		return movieId;
	}

	private static LocalDateTime day(int dayOfMonth) {
		return LocalDateTime.of(2026, 10, dayOfMonth, 12, 0);
	}

}
