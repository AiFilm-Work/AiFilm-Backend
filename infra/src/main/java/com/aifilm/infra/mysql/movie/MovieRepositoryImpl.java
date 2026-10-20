package com.aifilm.infra.mysql.movie;

import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import com.aifilm.domain.movie.Movie;
import com.aifilm.domain.movie.MovieRepository;

// domain의 MovieRepository를 Spring Data JPA로 구현한다.
@Repository
@RequiredArgsConstructor
public class MovieRepositoryImpl implements MovieRepository {

	private final JpaMovieRepository jpaMovieRepository;

	@Override
	public Movie save(Movie movie) {
		return jpaMovieRepository.save(movie);
	}

	@Override
	public Optional<Movie> findById(Long id) {
		return jpaMovieRepository.findById(id);
	}

}
