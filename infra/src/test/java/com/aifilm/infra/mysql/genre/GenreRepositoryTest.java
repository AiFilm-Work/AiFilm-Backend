package com.aifilm.infra.mysql.genre;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.aifilm.domain.genre.Genre;
import com.aifilm.domain.genre.GenreRepository;
import com.aifilm.domain.genre.MovieGenre;
import com.aifilm.infra.support.MovieFixture;
import com.aifilm.infra.support.RepositoryTest;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

@RepositoryTest
@Import(GenreRepositoryImpl.class)
class GenreRepositoryTest {

    private static final LocalDateTime APPROVED_AT = LocalDateTime.of(2026, 10, 20, 12, 0);

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("migration으로 넣은 장르 기준 데이터를 ID 순으로 조회한다")
    void findsSeededGenresInIdOrder() {
        List<Genre> genres = genreRepository.findAllOrderById();

        assertThat(genres).extracting(Genre::getId, Genre::getName)
                .containsExactly(
                        tuple(1L, "SF"),
                        tuple(2L, "공포"),
                        tuple(3L, "드라마"),
                        tuple(4L, "판타지"),
                        tuple(5L, "로맨스"),
                        tuple(6L, "코미디"),
                        tuple(7L, "애니메이션"));
    }

    @Test
    @DisplayName("새 장르는 기준 데이터 다음 ID로 저장된다")
    void savesNewGenreAfterSeedIds() {
        Genre saved = genreRepository.save(new Genre("다큐멘터리", null));

        assertThat(saved.getId()).isGreaterThan(7L);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("여러 영화의 장르 이름을 영화 ID, 장르 ID 순서로 한 번에 조회한다")
    void findsGenresOfMovies() {
        Long first = entityManager.persistAndFlush(MovieFixture.movie("genre-1", APPROVED_AT, 3L, 1L)).getId();
        Long second = entityManager.persistAndFlush(MovieFixture.movie("genre-2", APPROVED_AT, 2L)).getId();
        entityManager.persistAndFlush(MovieFixture.movie("genre-not-requested", APPROVED_AT, 4L));

        assertThat(genreRepository.findByMovieIds(List.of(first, second)))
                .containsExactly(
                        new MovieGenre(first, 1L, "SF"),
                        new MovieGenre(first, 3L, "드라마"),
                        new MovieGenre(second, 2L, "공포"));
    }

    @Test
    @DisplayName("영화 ID 목록이 비어 있으면 조회하지 않고 빈 목록을 돌려준다")
    void returnsEmptyForNoMovieIds() {
        assertThat(genreRepository.findByMovieIds(List.of())).isEmpty();
    }

}
