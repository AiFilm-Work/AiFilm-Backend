package com.aifilm.infra.mysql.movie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aifilm.domain.movie.PrivacyStatus;
import com.aifilm.domain.movie.YoutubeMovieSource;
import com.aifilm.domain.movie.YoutubeMovieSourceRepository;
import com.aifilm.infra.support.MovieFixture;
import com.aifilm.infra.support.RepositoryTest;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

@RepositoryTest
@Import(YoutubeMovieSourceRepositoryImpl.class)
class YoutubeMovieSourceRepositoryTest {

    private static final LocalDateTime APPROVED_AT = LocalDateTime.of(2026, 11, 16, 15, 12);

    @Autowired
    private YoutubeMovieSourceRepository youtubeMovieSourceRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Long movieId;

    @BeforeEach
    void setUp() {
        movieId = entityManager.persistAndFlush(MovieFixture.movie("youtube-movie", APPROVED_AT, 1L)).getId();
    }

    @Test
    void YouTube_출처를_영화_ID로_저장하고_다시_조회할_수_있다() {
        // given
        YoutubeMovieSource source = MovieFixture.youtubeMovieSource(movieId, "abc123");

        // when
        youtubeMovieSourceRepository.save(source);

        entityManager.flush();
        entityManager.clear();
        YoutubeMovieSource foundSource = youtubeMovieSourceRepository.findByMovieId(movieId).orElseThrow();

        // then
        assertThat(foundSource.getMovieId()).isEqualTo(movieId);
        assertThat(foundSource.getYoutubeVideoId()).isEqualTo("abc123");
        assertThat(foundSource.getPrivacyStatus()).isEqualTo(PrivacyStatus.PUBLIC);
    }

    @Test
    void 이미_YouTube_출처가_있는_영화에_YouTube_출처를_다시_저장하면_덮어쓰지_않고_실패한다() {
        // given
        YoutubeMovieSource firstSource = MovieFixture.youtubeMovieSource(movieId, "first-video");
        YoutubeMovieSource secondSource = MovieFixture.youtubeMovieSource(movieId, "second-video");

        youtubeMovieSourceRepository.save(firstSource);

        entityManager.flush();
        entityManager.clear();

        // when & then
        assertThatThrownBy(() -> {
            youtubeMovieSourceRepository.save(secondSource);
            entityManager.flush();
        }).hasRootCauseInstanceOf(SQLIntegrityConstraintViolationException.class);
    }

    @Test
    void 같은_YouTube_영상은_두_개의_영화로_중복_등록될_수_없다() {
        // given
        YoutubeMovieSource source = MovieFixture.youtubeMovieSource(movieId, "same_youtube_ID");
        youtubeMovieSourceRepository.save(source);
        entityManager.flush();

        Long otherMovieId = entityManager.persistAndFlush(MovieFixture.movie("other-movie", APPROVED_AT, 1L)).getId();

        // when & then
        assertThatThrownBy(() -> {
            youtubeMovieSourceRepository.save(
                    MovieFixture.youtubeMovieSource(otherMovieId, "same_youtube_ID")); // 같은 유튜브 영상(same_youtube_ID)
            entityManager.flush();
        }).hasRootCauseInstanceOf(SQLIntegrityConstraintViolationException.class);
    }
}
