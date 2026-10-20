package com.aifilm.domain.movie;

import java.util.Optional;

// 영화 저장소. 구현은 infra 모듈이 한다.
public interface MovieRepository {

	Movie save(Movie movie);

	Optional<Movie> findById(Long id);

}
