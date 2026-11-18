package com.aifilm.domain.genre;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

// 장르 저장소. 구현은 infra 모듈이 한다.
public interface GenreRepository {

    Genre save(Genre genre);

    Optional<Genre> findById(Long id);

    // 홈 장르 메뉴처럼 항상 같은 순서로 보여주기 위해 ID 순으로 반환한다
    List<Genre> findAllOrderById();

    // 여러 영화의 장르를 쿼리 한 번으로 조회한다. 영화 ID, 장르 ID 순으로 반환한다.
    List<MovieGenre> findByMovieIds(Collection<Long> movieIds);

}
