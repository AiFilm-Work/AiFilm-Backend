package com.aifilm.domain.movie;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 영화의 YouTube 출처 정보. movie와 PK를 공유하고(movie_id = movie.id), 한 영화에 최대 하나만 붙는다.
// Movie 객체 대신 movieId만 가진다. 영화와의 무결성은 DB FK(fk_youtube_movie_source_movie)가 지킨다.
@Entity
@Table(name = "youtube_movie_source")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class YoutubeMovieSource {

    // 저장된 Movie의 id를 그대로 쓴다. AUTO_INCREMENT가 아니다.
    @Id
    private Long movieId;

    @Column(nullable = false, length = 32)
    private String youtubeVideoId;

    @Column(nullable = false, length = 64)
    private String youtubeChannelId;

    @Column(nullable = false)
    private String youtubeChannelTitle;

    // 컬럼이 TEXT라서 스키마 검증(ddl-auto=validate)이 VARCHAR로 기대하지 않게 타입을 적는다
    @Column(columnDefinition = "TEXT")
    private String youtubeDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PrivacyStatus privacyStatus;

    @Column(nullable = false)
    private boolean embeddable;

    @Column(nullable = false)
    private boolean youtubeDeleted;

    // 원본 동기화가 확인한 YouTube 쪽 재생 가능 여부. 영화 전체의 재생 가능 여부는 Movie.availabilityStatus다
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private YoutubeAvailabilityStatus youtubeAvailabilityStatus;

    private LocalDateTime youtubePublishedAt;

    private LocalDateTime youtubeSyncedAt;

    // 승인한 영화의 YouTube 영상을 등록할 때 쓴다. 공개·임베드 가능·재생 가능한 영상으로 시작한다
    @Builder
    private YoutubeMovieSource(Long movieId, String youtubeVideoId, String youtubeChannelId,
            String youtubeChannelTitle, String youtubeDescription, LocalDateTime youtubePublishedAt) {
        if (movieId == null) {
            throw new IllegalArgumentException("영화 ID는 비어 있을 수 없습니다.");
        }
        this.movieId = movieId;
        this.youtubeVideoId = requireNonBlankText(youtubeVideoId, "YouTube 영상 ID");
        this.youtubeChannelId = requireNonBlankText(youtubeChannelId, "YouTube 채널 ID");
        this.youtubeChannelTitle = requireNonBlankText(youtubeChannelTitle, "YouTube 채널 이름");
        this.youtubeDescription = youtubeDescription;
        this.youtubePublishedAt = youtubePublishedAt;
        this.privacyStatus = PrivacyStatus.PUBLIC;
        this.embeddable = true;
        this.youtubeDeleted = false;
        this.youtubeAvailabilityStatus = YoutubeAvailabilityStatus.AVAILABLE;
    }

    private static String requireNonBlankText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + "은(는) 비어 있을 수 없습니다.");
        }
        return value;
    }

}
