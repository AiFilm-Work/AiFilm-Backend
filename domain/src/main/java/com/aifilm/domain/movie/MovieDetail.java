package com.aifilm.domain.movie;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

// 영화 상세 화면에 보여줄 값과 노출 상태를 담은 조회 모델. 엔티티를 불러오지 않고 필요한 컬럼만 조회해서 만든다.
// 상세는 없는 영화와 지금 볼 수 없는 영화를 나눠 응답하므로, 목록 조회와 달리 노출 상태도 함께 담는다.
// YouTube 값(설명, 영상 ID, 채널 이름, 게시 시각)은 youtube_movie_source에서 가져온다. YouTube 출처가 없는 영화면 null이다.
public record MovieDetail(Long movieId, String title, String shortDescription, String youtubeDescription,
        String thumbnailUrl, String youtubeVideoId, String youtubeChannelTitle, int runningTimeSeconds,
        LocalDateTime youtubePublishedAt, LocalDateTime approvedAt, BigDecimal averageRating, long reviewCount,
        long totalViewCount, ScreeningStatus screeningStatus, AvailabilityStatus availabilityStatus) {

    // 승인 후 이 기간이 지나기 전까지 NEW 배지를 붙인다
    public static final Duration NEW_BADGE_PERIOD = Duration.ofDays(7);

    // 노출 중(ACTIVE)이고 출처와 상관없이 재생할 수 있는(AVAILABLE) 영화만 사용자에게 보여준다
    public boolean isScreenable() {
        return screeningStatus == ScreeningStatus.ACTIVE
                && availabilityStatus == AvailabilityStatus.AVAILABLE;
    }

    // 승인 시각부터 기간이 꽉 차는 순간(정확히 7일)부터는 NEW가 아니다
    public boolean isNewAt(LocalDateTime now) {
        return now.isBefore(approvedAt.plus(NEW_BADGE_PERIOD));
    }

}
