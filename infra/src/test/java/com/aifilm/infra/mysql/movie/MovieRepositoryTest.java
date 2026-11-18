package com.aifilm.infra.mysql.movie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aifilm.domain.movie.AvailabilityStatus;
import com.aifilm.domain.movie.Movie;
import com.aifilm.domain.movie.MovieCard;
import com.aifilm.domain.movie.MovieDetail;
import com.aifilm.domain.movie.MovieRepository;
import com.aifilm.domain.movie.ScreeningStatus;
import com.aifilm.infra.support.MovieFixture;
import com.aifilm.infra.support.RepositoryTest;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

@RepositoryTest
@Import(MovieRepositoryImpl.class)
class MovieRepositoryTest {

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void 영화를_장르와_함께_저장하고_다시_조회할_수_있다() {
        // given
        Movie movie = MovieFixture.movie("with-genres", day(1), 1L, 3L);

        // when
        Movie saved = movieRepository.save(movie);
        // 1차 캐시가 아니라 DB에서 다시 읽어 오도록 비운다
        entityManager.flush();
        entityManager.clear();
        Movie found = movieRepository.findById(saved.getId()).orElseThrow();

        // then
        assertThat(found.getTitle()).isEqualTo("영화 with-genres");
        assertThat(found.getRunningTimeSeconds()).isEqualTo(600);
        assertThat(found.getScreeningStatus()).isEqualTo(ScreeningStatus.ACTIVE);
        assertThat(found.getGenreIds()).containsExactlyInAnyOrder(1L, 3L);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    void 없는_장르_ID로는_영화를_저장할_수_없다() {
        // given
        Movie movie = MovieFixture.movie("unknown-genre", day(1), 999L);

        // when & then
        assertThatThrownBy(() -> {
            movieRepository.save(movie);
            entityManager.flush();
        }).hasRootCauseInstanceOf(SQLIntegrityConstraintViolationException.class);
    }

    @Test
    void 최신_등록작은_노출_중이고_재생_가능한_영화만_승인_시각이_늦은_순서로_limit개까지_조회한다() {
        // given
        Long oldest = save("latest-1", day(1));
        Long middle = save("latest-2", day(2));
        Long newest = save("latest-3", day(3));
        Long hidden = save("latest-hidden", day(4));
        Long unavailable = save("latest-unavailable", day(5));
        MovieFixture.changeStatus(entityManager, hidden, ScreeningStatus.HIDDEN, AvailabilityStatus.AVAILABLE);
        MovieFixture.changeStatus(entityManager, unavailable,
                ScreeningStatus.ACTIVE, AvailabilityStatus.UNAVAILABLE);

        // when & then
        assertThat(movieRepository.findLatestScreenable(10))
                .extracting(MovieCard::movieId)
                .containsExactly(newest, middle, oldest);
        assertThat(movieRepository.findLatestScreenable(2))
                .extracting(MovieCard::movieId)
                .containsExactly(newest, middle);
    }

    @Test
    void 승인_시각이_같으면_나중에_등록한_영화가_먼저_나온다() {
        // given
        Long first = save("same-time-1", day(1));
        Long second = save("same-time-2", day(1));

        // when & then
        assertThat(movieRepository.findLatestScreenable(10))
                .extracting(MovieCard::movieId)
                .containsExactly(second, first);
    }

    @Test
    void 카드에는_목록에_보여줄_제목_썸네일_평점_리뷰_수가_담긴다() {
        // given
        Long movieId = save("card", day(1));
        MovieFixture.changeRating(entityManager, movieId, "4.50", 12);

        // when
        MovieCard card = movieRepository.findLatestScreenable(1).getFirst();

        // then
        assertThat(card.movieId()).isEqualTo(movieId);
        assertThat(card.title()).isEqualTo("영화 card");
        assertThat(card.thumbnailUrl()).isEqualTo("https://example.com/card.jpg");
        assertThat(card.averageRating()).isEqualByComparingTo("4.50");
        assertThat(card.reviewCount()).isEqualTo(12);
    }

    @Test
    void 평점_TOP은_평점이_높은_순서로_같으면_리뷰_수_그것도_같으면_승인_시각이_늦은_순서로_조회한다() {
        // given
        Long best = saveRated("rating-best", day(1), "4.80", 10);
        Long moreReviews = saveRated("rating-more-reviews", day(1), "4.50", 30);
        Long newer = saveRated("rating-newer", day(3), "4.50", 10);
        Long older = saveRated("rating-older", day(2), "4.50", 10);
        Long lowest = saveRated("rating-lowest", day(5), "3.00", 100);
        Long hidden = saveRated("rating-hidden", day(1), "5.00", 100);
        Long unavailable = saveRated("rating-unavailable", day(1), "5.00", 100);
        MovieFixture.changeStatus(entityManager, hidden, ScreeningStatus.HIDDEN, AvailabilityStatus.AVAILABLE);
        MovieFixture.changeStatus(entityManager, unavailable,
                ScreeningStatus.ACTIVE, AvailabilityStatus.UNAVAILABLE);

        // when & then
        assertThat(movieRepository.findTopRatedScreenable(10))
                .extracting(MovieCard::movieId)
                .containsExactly(best, moreReviews, newer, older, lowest);
        assertThat(movieRepository.findTopRatedScreenable(3))
                .extracting(MovieCard::movieId)
                .containsExactly(best, moreReviews, newer);
    }

    // 상세는 생성자 인자를 순서로 맞추므로, 값이 모두 채워져 있고 서로 달라야 자리가 바뀐 것을 잡을 수 있다
    @Test
    void 상세_조회는_상세_화면에_보여줄_값과_노출_상태를_담는다() {
        // given
        Long movieId = save("detail", day(20));
        entityManager.persistAndFlush(MovieFixture.youtubeMovieSource(movieId, "detail-video",
                "기억과 사랑을 주제로 제작한 AI 단편영화입니다.", LocalDateTime.of(2025, 1, 10, 13, 20)));
        MovieFixture.changeRating(entityManager, movieId, "4.70", 21);

        // when
        MovieDetail detail = movieRepository.findDetailById(movieId).orElseThrow();

        // then
        assertThat(detail.movieId()).isEqualTo(movieId);
        assertThat(detail.title()).isEqualTo("영화 detail");
        assertThat(detail.shortDescription()).isEqualTo("짧은 소개");
        assertThat(detail.youtubeDescription()).isEqualTo("기억과 사랑을 주제로 제작한 AI 단편영화입니다.");
        assertThat(detail.thumbnailUrl()).isEqualTo("https://example.com/detail.jpg");
        assertThat(detail.youtubeVideoId()).isEqualTo("detail-video");
        assertThat(detail.youtubeChannelTitle()).isEqualTo("AI 스튜디오");
        assertThat(detail.runningTimeSeconds()).isEqualTo(600);
        assertThat(detail.youtubePublishedAt()).isEqualTo(LocalDateTime.of(2025, 1, 10, 13, 20));
        assertThat(detail.approvedAt()).isEqualTo(day(20));
        assertThat(detail.averageRating()).isEqualByComparingTo("4.70");
        assertThat(detail.reviewCount()).isEqualTo(21);
        assertThat(detail.totalViewCount()).isZero();
        assertThat(detail.screeningStatus()).isEqualTo(ScreeningStatus.ACTIVE);
        assertThat(detail.availabilityStatus()).isEqualTo(AvailabilityStatus.AVAILABLE);
    }

    @Test
    void 상세_조회는_숨기거나_재생할_수_없는_영화도_상태와_함께_조회한다() {
        // given
        Long hidden = save("detail-hidden", day(1));
        Long unavailable = save("detail-unavailable", day(1));
        MovieFixture.changeStatus(entityManager, hidden, ScreeningStatus.HIDDEN, AvailabilityStatus.AVAILABLE);
        MovieFixture.changeStatus(entityManager, unavailable,
                ScreeningStatus.ACTIVE, AvailabilityStatus.UNAVAILABLE);

        // when & then
        assertThat(movieRepository.findDetailById(hidden).orElseThrow().screeningStatus())
                .isEqualTo(ScreeningStatus.HIDDEN);
        assertThat(movieRepository.findDetailById(unavailable).orElseThrow().availabilityStatus())
                .isEqualTo(AvailabilityStatus.UNAVAILABLE);
    }

    @Test
    void YouTube_설명과_게시일이_없는_영화는_두_값이_null로_조회된다() {
        // given
        Long movieId = MovieFixture.saveWithYoutube(entityManager, "detail-no-description", day(1), 1L);

        // when
        MovieDetail detail = movieRepository.findDetailById(movieId).orElseThrow();

        // then
        assertThat(detail.youtubeDescription()).isNull();
        assertThat(detail.youtubePublishedAt()).isNull();
    }

    // 상세 쿼리가 youtube_movie_source를 LEFT JOIN하는지 지킨다. INNER JOIN이면 이 영화의 상세가 빈 값이 된다
    @Test
    void YouTube_출처가_없는_영화도_상세는_조회되고_YouTube_값은_null이다() {
        // given
        Long movieId = save("detail-no-youtube", day(1));

        // when
        MovieDetail detail = movieRepository.findDetailById(movieId).orElseThrow();

        // then
        assertThat(detail.title()).isEqualTo("영화 detail-no-youtube");
        assertThat(detail.youtubeVideoId()).isNull();
        assertThat(detail.youtubeChannelTitle()).isNull();
    }

    @Test
    void 없는_영화는_빈_값으로_조회된다() {
        // when & then
        assertThat(movieRepository.findDetailById(Long.MAX_VALUE)).isEmpty();
    }

    // 홈 섹션 조회는 movie만 읽으므로 YouTube 출처 없이 영화만 저장한다
    private Long save(String key, LocalDateTime approvedAt) {
        return entityManager.persistAndFlush(MovieFixture.movie(key, approvedAt, 1L)).getId();
    }

    private Long saveRated(String key, LocalDateTime approvedAt, String averageRating, long reviewCount) {
        Long movieId = save(key, approvedAt);
        MovieFixture.changeRating(entityManager, movieId, averageRating, reviewCount);
        return movieId;
    }

    private static LocalDateTime day(int dayOfMonth) {
        return LocalDateTime.of(2026, 10, dayOfMonth, 12, 0);
    }

}
