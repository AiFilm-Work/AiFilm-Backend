package com.aifilm.infra.mysql.movie;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;

import com.aifilm.domain.movie.AvailabilityStatus;
import com.aifilm.domain.movie.Movie;
import com.aifilm.domain.movie.MovieCard;
import com.aifilm.domain.movie.MovieDetail;
import com.aifilm.domain.movie.MovieRepository;
import com.aifilm.domain.movie.ScreeningStatus;

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

	@Override
	public List<MovieCard> findLatestScreenable(int limit) {
		return jpaMovieRepository.findLatestCards(
			ScreeningStatus.ACTIVE, AvailabilityStatus.AVAILABLE, Limit.of(limit));
	}

	@Override
	public List<MovieCard> findTopRatedScreenable(int limit) {
		return jpaMovieRepository.findTopRatedCards(
			ScreeningStatus.ACTIVE, AvailabilityStatus.AVAILABLE, Limit.of(limit));
	}

	@Override
	public Optional<MovieDetail> findDetailById(Long id) {
		return jpaMovieRepository.findDetailById(id);
	}

}
