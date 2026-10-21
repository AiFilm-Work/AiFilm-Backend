package com.aifilm.infra.support;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.aifilm.domain.movie.Movie;
import com.aifilm.domain.movie.ScreeningStatus;
import com.aifilm.domain.movie.YoutubeAvailabilityStatus;

// 리포지토리 테스트에서 영화를 만들고 상태를 바꾸는 헬퍼. 테스트마다 다른 값은 영상 ID, 승인 시각, 장르뿐이다.
public final class MovieFixture {

	private MovieFixture() {
	}

	public static Movie movie(String youtubeVideoId, LocalDateTime approvedAt, Long... genreIds) {
		return Movie.builder()
			.youtubeVideoId(youtubeVideoId)
			.youtubeChannelId("channel-1")
			.youtubeChannelTitle("AI 스튜디오")
			.thumbnailUrl("https://example.com/" + youtubeVideoId + ".jpg")
			.title("영화 " + youtubeVideoId)
			.shortDescription("짧은 소개")
			.runningTimeSeconds(600)
			.approvedAt(approvedAt)
			.genreIds(Set.of(genreIds))
			.build();
	}

	// 노출 상태를 바꾸는 도메인 메서드가 아직 없어서 SQL로 바꾼다. 영화는 먼저 DB에 반영(flush)해 둬야 한다.
	public static void changeStatus(TestEntityManager entityManager, Long movieId, ScreeningStatus screeningStatus,
		YoutubeAvailabilityStatus availabilityStatus) {
		entityManager.getEntityManager()
			.createNativeQuery("UPDATE movie SET screening_status = ?, youtube_availability_status = ? WHERE id = ?")
			.setParameter(1, screeningStatus.name())
			.setParameter(2, availabilityStatus.name())
			.setParameter(3, movieId)
			.executeUpdate();
	}

	// 평점은 리뷰 기능이 생기면 리뷰로 계산한다. 그 전까지 테스트에서는 SQL로 바꾼다.
	public static void changeRating(TestEntityManager entityManager, Long movieId, String averageRating,
		long reviewCount) {
		entityManager.getEntityManager()
			.createNativeQuery("UPDATE movie SET average_rating = ?, review_count = ? WHERE id = ?")
			.setParameter(1, new BigDecimal(averageRating))
			.setParameter(2, reviewCount)
			.setParameter(3, movieId)
			.executeUpdate();
	}

}
