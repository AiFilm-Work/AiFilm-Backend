package com.aifilm.infra.mysql.genre;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.aifilm.domain.genre.Genre;
import com.aifilm.domain.genre.GenreRepository;

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

}
