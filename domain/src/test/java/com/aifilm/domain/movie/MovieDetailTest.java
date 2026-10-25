package com.aifilm.domain.movie;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MovieDetailTest {

	private static final LocalDateTime APPROVED_AT = LocalDateTime.of(2026, 10, 20, 12, 0);

	@ParameterizedTest
	@CsvSource({
		"ACTIVE, AVAILABLE, true",
		"HIDDEN, AVAILABLE, false",
		"UNAVAILABLE, AVAILABLE, false",
		"ACTIVE, UNAVAILABLE, false"
	})
	@DisplayName("노출 중이고 YouTube에서 재생할 수 있는 영화만 보여줄 수 있다")
	void screenableOnlyWhenActiveAndAvailable(ScreeningStatus screeningStatus,
		YoutubeAvailabilityStatus availabilityStatus, boolean expected) {
		assertThat(detail(screeningStatus, availabilityStatus).isScreenable()).isEqualTo(expected);
	}

	@Test
	@DisplayName("승인 후 7일이 지나기 직전까지 NEW이고, 정확히 7일이 되면 NEW가 아니다")
	void newUntilSevenDaysAfterApproval() {
		MovieDetail detail = detail(ScreeningStatus.ACTIVE, YoutubeAvailabilityStatus.AVAILABLE);

		assertThat(detail.isNewAt(APPROVED_AT)).isTrue();
		assertThat(detail.isNewAt(APPROVED_AT.plusDays(7).minusSeconds(1))).isTrue();
		assertThat(detail.isNewAt(APPROVED_AT.plusDays(7))).isFalse();
	}

	private static MovieDetail detail(ScreeningStatus screeningStatus, YoutubeAvailabilityStatus availabilityStatus) {
		return new MovieDetail(1L, "네온 메모리", "짧은 소개", null, "https://example.com/1.jpg", "abc123",
			"AI 스튜디오", 600, null, APPROVED_AT, BigDecimal.ZERO, 0, 0, screeningStatus, availabilityStatus);
	}

}
