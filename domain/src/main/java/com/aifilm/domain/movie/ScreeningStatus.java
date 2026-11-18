package com.aifilm.domain.movie;

// AI FILM 서비스에 노출할지. ACTIVE인 영화만 목록과 홈에 보인다.
public enum ScreeningStatus {
    ACTIVE,
    UNAVAILABLE,
    HIDDEN
}
