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

import com.aifilm.domain.movie.Movie;
import com.aifilm.domain.movie.MovieRepository;
import com.aifilm.domain.movie.ScreeningStatus;
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

}
