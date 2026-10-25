package com.aifilm.api.genre.dto;

import com.aifilm.domain.genre.Genre;
import com.aifilm.domain.genre.MovieGenre;

// 장르 하나. 홈 장르 메뉴, 홈 배너와 영화 상세의 장르 목록에 함께 쓴다.
public record GenreResponse(Long genreId, String name) {

	public static GenreResponse from(Genre genre) {
		return new GenreResponse(genre.getId(), genre.getName());
	}

	public static GenreResponse from(MovieGenre movieGenre) {
		return new GenreResponse(movieGenre.genreId(), movieGenre.name());
	}

}
