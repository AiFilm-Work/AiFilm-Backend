package com.aifilm.infra.mysql.genre;

import com.aifilm.domain.genre.Genre;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Spring Data JPA 리포지토리. 서비스 코드는 이 인터페이스 대신 domain의 GenreRepository를 쓴다.
public interface JpaGenreRepository extends JpaRepository<Genre, Long> {

    // Movie.genreIds는 엔티티가 아닌 값 컬렉션이라, movie를 거치지 않고 movie_genre와 genre를 바로 조인하는 SQL로 둔다.
    // movie_genre의 PK(movie_id, genre_id)를 그대로 탄다.
    @Query(value = """
            SELECT mg.movie_id AS movieId, g.id AS genreId, g.name AS name
            FROM movie_genre mg
                     JOIN genre g ON g.id = mg.genre_id
            WHERE mg.movie_id IN (:movieIds)
            ORDER BY mg.movie_id, g.id
            """, nativeQuery = true)
    List<MovieGenreRow> findMovieGenreRows(@Param("movieIds") Collection<Long> movieIds);

    // 네이티브 쿼리 결과를 받는 프로젝션. 구현에서 domain의 MovieGenre로 바꿔 내보낸다.
    interface MovieGenreRow {

        Long getMovieId();

        Long getGenreId();

        String getName();

    }

}
