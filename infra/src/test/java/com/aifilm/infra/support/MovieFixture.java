package com.aifilm.infra.support;

import com.aifilm.domain.movie.AvailabilityStatus;
import com.aifilm.domain.movie.Movie;
import com.aifilm.domain.movie.ScreeningStatus;
import com.aifilm.domain.movie.YoutubeMovieSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

// 리포지토리 테스트에서 영화와 YouTube 출처를 만들고 상태를 바꾸는 헬퍼. 테스트마다 다른 값은 이름(key), 승인 시각, 장르뿐이다.
public final class MovieFixture {

    private MovieFixture() {
    }

    // key는 제목과 썸네일 URL에 들어간다. 홈 섹션처럼 movie만 읽는 조회는 이 영화만 저장해도 확인할 수 있다
    public static Movie movie(String key, LocalDateTime approvedAt, Long... genreIds) {
        return Movie.builder()
                .thumbnailUrl("https://example.com/" + key + ".jpg")
                .title("영화 " + key)
                .shortDescription("짧은 소개")
                .runningTimeSeconds(600)
                .approvedAt(approvedAt)
                .genreIds(Set.of(genreIds))
                .build();
    }

    // 영상 ID는 영화마다 달라야 한다(uk_youtube_movie_source_youtube_video_id). 설명과 게시일은 비워 둔다
    public static YoutubeMovieSource youtubeMovieSource(Long movieId, String youtubeVideoId) {
        return youtubeMovieSource(movieId, youtubeVideoId, null, null);
    }

    // 상세 조회처럼 YouTube 값이 모두 채워져 있어야 하는 테스트에서 쓴다
    public static YoutubeMovieSource youtubeMovieSource(Long movieId, String youtubeVideoId, String youtubeDescription,
            LocalDateTime youtubePublishedAt) {
        return YoutubeMovieSource.builder()
                .movieId(movieId)
                .youtubeVideoId(youtubeVideoId)
                .youtubeChannelId("channel-1")
                .youtubeChannelTitle("AI 스튜디오")
                .youtubeDescription(youtubeDescription)
                .youtubePublishedAt(youtubePublishedAt)
                .build();
    }

    // 운영 데이터처럼 영화와 YouTube 출처를 함께 저장하고 영화 ID를 돌려준다. 영상 ID에는 key를 쓴다
    public static Long saveWithYoutube(TestEntityManager entityManager, String key, LocalDateTime approvedAt,
            Long... genreIds) {
        Long movieId = entityManager.persistAndFlush(movie(key, approvedAt, genreIds)).getId();
        entityManager.persistAndFlush(youtubeMovieSource(movieId, key));
        return movieId;
    }

    // 노출 상태를 바꾸는 도메인 메서드가 아직 없어서 SQL로 바꾼다. 영화는 먼저 DB에 반영(flush)해 둬야 한다.
    // 노출 조건은 availability_status만 보므로 youtube_availability_status는 바꾸지 않는다.
    // 두 값이 엇갈려도 availability_status를 따라 걸러지는지가 함께 확인된다.
    public static void changeStatus(TestEntityManager entityManager, Long movieId, ScreeningStatus screeningStatus,
            AvailabilityStatus availabilityStatus) {
        entityManager.getEntityManager()
                .createNativeQuery("UPDATE movie SET screening_status = ?, availability_status = ? WHERE id = ?")
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
