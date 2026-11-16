package com.aifilm.infra.mysql.movie;

import java.util.Optional;

import jakarta.persistence.EntityManager;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import com.aifilm.domain.movie.YoutubeMovieSource;
import com.aifilm.domain.movie.YoutubeMovieSourceRepository;

// domain의 YoutubeMovieSourceRepository를 EntityManager로 구현한다.
// movieId를 직접 넣는 엔티티라 Spring Data의 save()는 새 엔티티로 보지 않고 merge한다. merge는 같은 movieId 행이 있으면
// 오류 없이 덮어쓴다. 여기서 save()는 새 행을 만드는 메서드이므로 persist로 넣고, 중복이면 PK 위반으로 실패하게 한다.
@Repository
@RequiredArgsConstructor
public class YoutubeMovieSourceRepositoryImpl implements YoutubeMovieSourceRepository {

	private final EntityManager entityManager;

	@Override
	public YoutubeMovieSource save(YoutubeMovieSource youtubeMovieSource) {
		entityManager.persist(youtubeMovieSource);
		return youtubeMovieSource;
	}

	@Override
	public Optional<YoutubeMovieSource> findByMovieId(Long movieId) {
		return Optional.ofNullable(entityManager.find(YoutubeMovieSource.class, movieId));
	}

}
