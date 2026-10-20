package com.aifilm.domain.movie;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 관리자가 승인해 서비스에 등록된 AI 영화. 영상은 저장하지 않고 YouTube 영상 ID로 임베드 재생한다.
@Entity
@Table(name = "movie")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Movie {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 32)
	private String youtubeVideoId;

	@Column(nullable = false, length = 64)
	private String youtubeChannelId;

	@Column(nullable = false)
	private String youtubeChannelTitle;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PrivacyStatus privacyStatus;

	@Column(nullable = false)
	private boolean embeddable;

	@Column(nullable = false)
	private boolean youtubeDeleted;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private YoutubeAvailabilityStatus youtubeAvailabilityStatus;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ScreeningStatus screeningStatus;

	@Column(nullable = false, length = 2048)
	private String thumbnailUrl;

	@Column(nullable = false, length = 500)
	private String title;

	// 컬럼이 TEXT라서 스키마 검증(ddl-auto=validate)이 VARCHAR로 기대하지 않게 타입을 적는다
	@Column(columnDefinition = "TEXT")
	private String youtubeDescription;

	@Column(nullable = false)
	private String shortDescription;

	@Column(nullable = false)
	private int runningTimeSeconds;

	private LocalDateTime youtubePublishedAt;

	@Column(nullable = false, precision = 3, scale = 2)
	private BigDecimal averageRating;

	@Column(nullable = false)
	private long reviewCount;

	@Column(nullable = false)
	private long totalViewCount;

	private LocalDateTime youtubeSyncedAt;

	@Column(nullable = false)
	private LocalDateTime approvedAt;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	// 장르는 별도 애그리거트라 엔티티 대신 ID로만 참조한다. 장르 정보가 필요한 조회는 조회 쿼리에서 조인한다.
	@ElementCollection
	@CollectionTable(name = "movie_genre", joinColumns = @JoinColumn(name = "movie_id"))
	@Column(name = "genre_id", nullable = false)
	private Set<Long> genreIds = new LinkedHashSet<>();

	// 승인된 영화를 등록할 때 쓴다. YouTube 상태는 등록 시점에 재생 가능한 것으로 시작하고, 평점·조회수는 0에서 시작한다.
	@Builder
	private Movie(String youtubeVideoId, String youtubeChannelId, String youtubeChannelTitle, String thumbnailUrl,
		String title, String youtubeDescription, String shortDescription, int runningTimeSeconds,
		LocalDateTime youtubePublishedAt, LocalDateTime approvedAt, Set<Long> genreIds) {
		this.youtubeVideoId = requireText(youtubeVideoId, "YouTube 영상 ID");
		this.youtubeChannelId = requireText(youtubeChannelId, "YouTube 채널 ID");
		this.youtubeChannelTitle = requireText(youtubeChannelTitle, "YouTube 채널 이름");
		this.thumbnailUrl = requireText(thumbnailUrl, "썸네일 URL");
		this.title = requireText(title, "제목");
		this.shortDescription = requireText(shortDescription, "짧은 소개");
		this.youtubeDescription = youtubeDescription;
		if (runningTimeSeconds <= 0) {
			throw new IllegalArgumentException("러닝타임은 1초 이상이어야 합니다.");
		}
		this.runningTimeSeconds = runningTimeSeconds;
		if (approvedAt == null) {
			throw new IllegalArgumentException("승인 시각은 비어 있을 수 없습니다.");
		}
		this.approvedAt = approvedAt;
		this.youtubePublishedAt = youtubePublishedAt;
		if (genreIds != null) {
			this.genreIds.addAll(genreIds);
		}
		this.privacyStatus = PrivacyStatus.PUBLIC;
		this.embeddable = true;
		this.youtubeDeleted = false;
		this.youtubeAvailabilityStatus = YoutubeAvailabilityStatus.AVAILABLE;
		this.screeningStatus = ScreeningStatus.ACTIVE;
		this.averageRating = BigDecimal.ZERO;
		this.reviewCount = 0;
		this.totalViewCount = 0;
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

	private static String requireText(String value, String name) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(name + "은(는) 비어 있을 수 없습니다.");
		}
		return value;
	}

}
