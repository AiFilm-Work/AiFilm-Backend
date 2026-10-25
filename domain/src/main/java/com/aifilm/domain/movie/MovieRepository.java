package com.aifilm.domain.movie;

import java.util.List;
import java.util.Optional;

// 영화 저장소. 구현은 infra 모듈이 한다.
public interface MovieRepository {

	Movie save(Movie movie);

	Optional<Movie> findById(Long id);

	// 노출 중(ACTIVE)이고 재생 가능한(AVAILABLE) 영화를 승인 시각이 늦은 순서로 limit개까지 조회한다
	List<MovieCard> findLatestScreenable(int limit);

	// 노출 중이고 재생 가능한 영화를 평점 → 리뷰 수 → 승인 시각 순서로 limit개까지 조회한다
	List<MovieCard> findTopRatedScreenable(int limit);

	// 노출 상태와 상관없이 영화 한 편의 상세 값을 조회한다. 보여줄 수 있는지는 호출하는 쪽이 상태로 판단한다
	Optional<MovieDetail> findDetailById(Long id);

}
