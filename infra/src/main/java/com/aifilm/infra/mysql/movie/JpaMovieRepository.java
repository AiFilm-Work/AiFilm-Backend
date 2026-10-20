package com.aifilm.infra.mysql.movie;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aifilm.domain.movie.Movie;

// Spring Data JPA 리포지토리. 서비스 코드는 이 인터페이스 대신 domain의 MovieRepository를 쓴다.
public interface JpaMovieRepository extends JpaRepository<Movie, Long> {
}
