package com.aifilm.domain.movie;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// 영화 상세 화면에 보여줄 값과 노출 상태를 담은 조회 모델. 엔티티를 불러오지 않고 필요한 컬럼만 조회해서 만든다.
// 상세는 없는 영화와 지금 볼 수 없는 영화를 나눠 응답하므로, 목록 조회와 달리 노출 상태도 함께 담는다.
public record MovieDetail(Long movieId, String title, String shortDescription, String youtubeDescription,
	String thumbnailUrl, String youtubeVideoId, String youtubeChannelTitle, int runningTimeSeconds,
	LocalDateTime youtubePublishedAt, LocalDateTime approvedAt, BigDecimal averageRating, long reviewCount,
	long totalViewCount, ScreeningStatus screeningStatus, YoutubeAvailabilityStatus youtubeAvailabilityStatus) {
}
