package com.aifilm.domain.genre;

// 영화 한 편에 연결된 장르 하나. 여러 영화의 장르 이름을 한 번에 조회할 때 쓴다.
public record MovieGenre(Long movieId, Long genreId, String name) {
}
