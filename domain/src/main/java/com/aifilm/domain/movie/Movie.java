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

// 관리자가 승인해 서비스에 등록된 AI 영화의 공통정보. 영상 출처(Youtube/Uploaded)는 출처별 엔티티가 해당 Movie.id를 movie_id로 따로 가진다.
@Entity
@Table(name = "movie")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Movie {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// 관리자가 정하는 노출 여부. 사용자에게 보여줄지는 이 값과 availabilityStatus를 함께 보고 정한다
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ScreeningStatus screeningStatus;

	// 출처와 상관없이 지금 재생할 수 있는지. 출처들의 상태를 종합해 서비스가 갱신한다.
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AvailabilityStatus availabilityStatus;

	@Column(nullable = false, length = 2048)
	private String thumbnailUrl;

	@Column(nullable = false, length = 500)
	private String title;

	@Column(nullable = false)
	private String shortDescription;

	@Column(nullable = false)
	private int runningTimeSeconds;

	@Column(nullable = false, precision = 3, scale = 2)
	private BigDecimal averageRating;

	@Column(nullable = false)
	private long reviewCount;

	@Column(nullable = false)
	private long totalViewCount;

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

	// 승인된 영화를 등록할 때 쓴다. 노출 중, 재생 가능 상태로 시작하고, 평점·조회수는 0에서 시작한다.
	// YouTube 정보는 YoutubeMovieSource로 따로 저장한다.
	@Builder
	private Movie(String thumbnailUrl, String title, String shortDescription, int runningTimeSeconds,
		LocalDateTime approvedAt, Set<Long> genreIds) {
		this.thumbnailUrl = requireText(thumbnailUrl, "썸네일 URL");
		this.title = requireText(title, "제목");
		this.shortDescription = requireText(shortDescription, "짧은 소개");
		if (runningTimeSeconds <= 0) {
			throw new IllegalArgumentException("러닝타임은 1초 이상이어야 합니다.");
		}
		this.runningTimeSeconds = runningTimeSeconds;
		if (approvedAt == null) {
			throw new IllegalArgumentException("승인 시각은 비어 있을 수 없습니다.");
		}
		this.approvedAt = approvedAt;
		if (genreIds != null) {
			this.genreIds.addAll(genreIds);
		}
		this.screeningStatus = ScreeningStatus.ACTIVE;
		this.availabilityStatus = AvailabilityStatus.AVAILABLE;
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
