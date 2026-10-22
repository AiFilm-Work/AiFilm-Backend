package com.aifilm.infra.mysql.genre;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.aifilm.domain.genre.Genre;
import com.aifilm.domain.genre.GenreRepository;
import com.aifilm.domain.genre.MovieGenre;

// domain의 GenreRepository를 Spring Data JPA로 구현한다.
@Repository
@RequiredArgsConstructor
public class GenreRepositoryImpl implements GenreRepository {

	private final JpaGenreRepository jpaGenreRepository;

	@Override
	public Genre save(Genre genre) {
		return jpaGenreRepository.save(genre);
	}

	@Override
	public Optional<Genre> findById(Long id) {
		return jpaGenreRepository.findById(id);
	}

	@Override
	public List<Genre> findAllOrderById() {
		return jpaGenreRepository.findAll(Sort.by("id"));
	}

	@Override
	public List<MovieGenre> findByMovieIds(Collection<Long> movieIds) {
		// IN ()은 SQL 문법 오류라 영화 ID가 없으면 조회하지 않는다
		if (movieIds.isEmpty()) {
			return List.of();
		}
		return jpaGenreRepository.findMovieGenreRows(movieIds).stream()
			.map(row -> new MovieGenre(row.getMovieId(), row.getGenreId(), row.getName()))
			.toList();
	}

}
