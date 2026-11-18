package com.aifilm.domain.movie;

// YouTube 원본 영상을 지금 재생할 수 있는지. 원본 상태 동기화(Worker)가 갱신한다.
public enum YoutubeAvailabilityStatus {
    AVAILABLE,
    UNAVAILABLE
}
