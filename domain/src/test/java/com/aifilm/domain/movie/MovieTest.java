package com.aifilm.domain.movie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class MovieTest {

	private static Movie.MovieBuilder validMovie() {
		return Movie.builder()
			.youtubeVideoId("abc123")
			.youtubeChannelId("channel-1")
			.youtubeChannelTitle("AI 스튜디오")
			.thumbnailUrl("https://example.com/neon-memory.jpg")
			.title("네온 메모리")
			.shortDescription("기억은 사라져도, 사랑은 남는다.")
			.runningTimeSeconds(766)
			.approvedAt(LocalDateTime.of(2026, 10, 20, 12, 0))
			.genreIds(Set.of(1L, 3L));
	}

	@Test
	@DisplayName("등록한 영화는 노출 가능, 재생 가능 상태로 시작하고 평점과 조회수는 0이다")
	void startsActiveAndAvailable() {
		Movie movie = validMovie().build();

		assertThat(movie.getScreeningStatus()).isEqualTo(ScreeningStatus.ACTIVE);
		assertThat(movie.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.AVAILABLE);
		assertThat(movie.getYoutubeAvailabilityStatus()).isEqualTo(YoutubeAvailabilityStatus.AVAILABLE);
		assertThat(movie.getPrivacyStatus()).isEqualTo(PrivacyStatus.PUBLIC);
		assertThat(movie.isEmbeddable()).isTrue();
		assertThat(movie.isYoutubeDeleted()).isFalse();
		assertThat(movie.getAverageRating()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(movie.getReviewCount()).isZero();
		assertThat(movie.getTotalViewCount()).isZero();
		assertThat(movie.getGenreIds()).containsExactlyInAnyOrder(1L, 3L);
	}

	@ParameterizedTest
	@ValueSource(ints = {0, -1})
	@DisplayName("러닝타임이 0초 이하면 만들 수 없다")
	void rejectsNonPositiveRunningTime(int runningTimeSeconds) {
		assertThatThrownBy(() -> validMovie().runningTimeSeconds(runningTimeSeconds).build())
			.isInstanceOf(IllegalArgumentException.class);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = " ")
	@DisplayName("YouTube 영상 ID나 제목이 비어 있으면 만들 수 없다")
	void rejectsBlankRequiredText(String blank) {
		assertThatThrownBy(() -> validMovie().youtubeVideoId(blank).build())
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> validMovie().title(blank).build())
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("승인 시각이 없으면 만들 수 없다")
	void rejectsMissingApprovedAt() {
		assertThatThrownBy(() -> validMovie().approvedAt(null).build())
			.isInstanceOf(IllegalArgumentException.class);
	}

}
