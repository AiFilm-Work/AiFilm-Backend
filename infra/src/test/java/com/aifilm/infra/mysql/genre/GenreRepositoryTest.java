package com.aifilm.infra.mysql.genre;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import com.aifilm.domain.genre.Genre;
import com.aifilm.domain.genre.GenreRepository;
import com.aifilm.infra.support.RepositoryTest;

@RepositoryTest
@Import(GenreRepositoryImpl.class)
class GenreRepositoryTest {

	@Autowired
	private GenreRepository genreRepository;

	@Test
	@DisplayName("migration으로 넣은 장르 기준 데이터를 ID 순으로 조회한다")
	void findsSeededGenresInIdOrder() {
		List<Genre> genres = genreRepository.findAllOrderById();

		assertThat(genres).extracting(Genre::getId, Genre::getName)
			.containsExactly(
				tuple(1L, "SF"),
				tuple(2L, "공포"),
				tuple(3L, "드라마"),
				tuple(4L, "판타지"),
				tuple(5L, "로맨스"),
				tuple(6L, "코미디"),
				tuple(7L, "애니메이션"));
	}

	@Test
	@DisplayName("새 장르는 기준 데이터 다음 ID로 저장된다")
	void savesNewGenreAfterSeedIds() {
		Genre saved = genreRepository.save(new Genre("다큐멘터리", null));

		assertThat(saved.getId()).isGreaterThan(7L);
		assertThat(saved.getCreatedAt()).isNotNull();
	}

}
