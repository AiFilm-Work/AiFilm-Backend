package com.aifilm.domain.movie;

import java.util.Optional;

// 영화의 YouTube 출처 저장소. 구현은 infra 모듈이 한다.
public interface YoutubeMovieSourceRepository {

	// 새 출처를 저장한다. 같은 영화에 이미 YouTube 출처가 있으면 덮어쓰지 않고 실패한다
	YoutubeMovieSource save(YoutubeMovieSource youtubeMovieSource);

	Optional<YoutubeMovieSource> findByMovieId(Long movieId);

}
