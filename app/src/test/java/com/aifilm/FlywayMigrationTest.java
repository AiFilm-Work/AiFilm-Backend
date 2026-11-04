package com.aifilm;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
	@ValueSource(strings = {"flyway_schema_history", "genre", "movie", "movie_genre", "home_banner_movie",
		"youtube_movie"})
	@DisplayName("앱이 뜨면 Flyway가 실행되어 스키마 이력과 영화·장르·홈 배너·YouTube 영화 테이블이 생긴다")
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

	// 컬럼 순서가 바뀌면 정렬에 인덱스를 쓰지 못하므로 이름뿐 아니라 순서까지 확인한다
	@Test
	@DisplayName("홈 섹션 정렬용 인덱스가 노출 조건 컬럼, 정렬 컬럼 순서로 만들어진다")
	void createsHomeSectionIndexes() throws SQLException {
		assertThat(indexColumns("idx_movie_screening_availability_approved_at"))
			.containsExactly("screening_status", "youtube_availability_status", "approved_at");
		assertThat(indexColumns("idx_movie_screening_availability_rating"))
			.containsExactly("screening_status", "youtube_availability_status", "average_rating", "review_count",
				"approved_at");
	}

	// 출처와 무관한 재생 가능 여부(availability_status)로 홈 섹션을 거르는 인덱스. 구조는 V4 인덱스와 같다
	@Test
	@DisplayName("홈 섹션 정렬용 인덱스가 출처와 무관한 노출 조건 컬럼으로도 만들어진다")
	void createsHomeSectionIndexesOnAvailabilityStatus() throws SQLException {
		assertThat(indexColumns("idx_movie_screening_availability_status_approved_at"))
			.containsExactly("screening_status", "availability_status", "approved_at");
		assertThat(indexColumns("idx_movie_screening_availability_status_rating"))
			.containsExactly("screening_status", "availability_status", "average_rating", "review_count",
				"approved_at");
	}

	private List<String> indexColumns(String indexName) throws SQLException {
		List<String> columns = new ArrayList<>();
		try (Connection connection = dataSource.getConnection();
			PreparedStatement statement = connection.prepareStatement("""
				SELECT column_name
				FROM information_schema.statistics
				WHERE table_schema = DATABASE() AND table_name = 'movie' AND index_name = ?
				ORDER BY seq_in_index
				""")) {
			statement.setString(1, indexName);
			try (ResultSet rows = statement.executeQuery()) {
				while (rows.next()) {
					columns.add(rows.getString(1));
				}
			}
		}
		return columns;
	}

}
