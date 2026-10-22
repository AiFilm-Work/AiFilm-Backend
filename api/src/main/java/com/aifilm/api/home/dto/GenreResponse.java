package com.aifilm.api.home.dto;

import com.aifilm.domain.genre.Genre;
import com.aifilm.domain.genre.MovieGenre;

// 장르 메뉴와 배너 영화의 장르에 함께 쓴다.
public record GenreResponse(Long genreId, String name) {

	public static GenreResponse from(Genre genre) {
		return new GenreResponse(genre.getId(), genre.getName());
	}

	public static GenreResponse from(MovieGenre movieGenre) {
		return new GenreResponse(movieGenre.genreId(), movieGenre.name());
	}

}
