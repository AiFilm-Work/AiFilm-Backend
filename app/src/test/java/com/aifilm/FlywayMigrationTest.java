package com.aifilm;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import com.aifilm.support.IntegrationTest;

// 앱이 뜰 때 Flyway가 실행되어 테스트 MySQL 컨테이너에 migration이 적용되는지 확인한다.
// app은 infra에 컴파일 의존이 없으므로 Flyway API 대신 JDBC 메타데이터로 확인한다.
@IntegrationTest
class FlywayMigrationTest {

	@Autowired
	private DataSource dataSource;

	@ParameterizedTest
	@ValueSource(strings = {"flyway_schema_history", "genre", "movie", "movie_genre"})
	@DisplayName("앱이 뜨면 Flyway가 실행되어 스키마 이력과 영화·장르 테이블이 생긴다")
	void createsTables(String tableName) throws SQLException {
		try (Connection connection = dataSource.getConnection();
			ResultSet tables = connection.getMetaData()
				.getTables(connection.getCatalog(), null, tableName, new String[] {"TABLE"})) {
			assertThat(tables.next()).isTrue();
		}
	}

	@Test
	@DisplayName("장르 기준 데이터가 정해진 ID로 들어간다")
	void insertsGenres() throws SQLException {
		Map<Long, String> genres = new LinkedHashMap<>();
		try (Connection connection = dataSource.getConnection();
			Statement statement = connection.createStatement();
			ResultSet rows = statement.executeQuery("SELECT id, name FROM genre WHERE id <= 7 ORDER BY id")) {
			while (rows.next()) {
				genres.put(rows.getLong("id"), rows.getString("name"));
			}
		}

		assertThat(genres).containsExactly(
			Map.entry(1L, "SF"),
			Map.entry(2L, "공포"),
			Map.entry(3L, "드라마"),
			Map.entry(4L, "판타지"),
			Map.entry(5L, "로맨스"),
			Map.entry(6L, "코미디"),
			Map.entry(7L, "애니메이션"));
	}

	@Test
	@DisplayName("통합 테스트 DB에는 로컬 샘플 영화가 들어가지 않는다")
	void doesNotInsertLocalSampleMovies() throws SQLException {
		try (Connection connection = dataSource.getConnection();
			Statement statement = connection.createStatement();
			ResultSet rows = statement.executeQuery(
				"SELECT COUNT(*) FROM movie WHERE youtube_video_id LIKE 'local-sample-%'")) {
			rows.next();
			assertThat(rows.getLong(1)).isZero();
		}
	}
}
