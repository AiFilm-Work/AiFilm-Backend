package com.aifilm.domain.home;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 홈 상단 배너. 영화의 일부 구간을 미리보기로 재생한다. 영화는 별도 애그리거트라 ID로만 참조한다.
@Entity
@Table(name = "home_banner_movie")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HomeBanner {

    private static final int DEFAULT_START_SECONDS = 0;
    private static final int DEFAULT_PREVIEW_DURATION_SECONDS = 10;
    private static final boolean DEFAULT_ACTIVE = true;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long movieId;

    @Column(nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private int startSeconds;

    @Column(nullable = false)
    private int previewDurationSeconds;

    @Column(nullable = false)
    private boolean active;

    // 비어 있으면 바로 노출한다
    private LocalDateTime startsAt;

    // 비어 있으면 종료 없이 노출한다
    private LocalDateTime endsAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // 미리보기 구간과 활성 여부는 값을 주지 않으면 API 명세의 기본값(0초부터 10초, 활성)을 쓴다.
    // 미리보기 끝(시작 + 길이)이 러닝타임을 넘지 않는지는 영화 정보가 필요해서 배너를 등록하는 서비스에서 검증한다.
    @Builder
    private HomeBanner(Long movieId, int displayOrder, Integer startSeconds, Integer previewDurationSeconds,
            Boolean active, LocalDateTime startsAt, LocalDateTime endsAt) {
        if (movieId == null) {
            throw new IllegalArgumentException("영화 ID는 비어 있을 수 없습니다.");
        }
        if (displayOrder < 1) {
            throw new IllegalArgumentException("노출 순서는 1 이상이어야 합니다.");
        }
        int start = startSeconds == null ? DEFAULT_START_SECONDS : startSeconds;
        if (start < 0) {
            throw new IllegalArgumentException("미리보기 시작 위치는 0초 이상이어야 합니다.");
        }
        int duration = previewDurationSeconds == null ? DEFAULT_PREVIEW_DURATION_SECONDS : previewDurationSeconds;
        if (duration <= 0) {
            throw new IllegalArgumentException("미리보기 길이는 1초 이상이어야 합니다.");
        }
        if (startsAt != null && endsAt != null && !startsAt.isBefore(endsAt)) {
            throw new IllegalArgumentException("노출 종료 시각은 시작 시각보다 뒤여야 합니다.");
        }
        this.movieId = movieId;
        this.displayOrder = displayOrder;
        this.startSeconds = start;
        this.previewDurationSeconds = duration;
        this.active = active == null ? DEFAULT_ACTIVE : active;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

}
