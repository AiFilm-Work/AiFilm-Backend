package com.aifilm.infra.mysql.genre;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aifilm.domain.genre.Genre;

// Spring Data JPA 리포지토리. 서비스 코드는 이 인터페이스 대신 domain의 GenreRepository를 쓴다.
public interface JpaGenreRepository extends JpaRepository<Genre, Long> {
}
