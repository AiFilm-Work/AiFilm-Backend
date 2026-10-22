package com.aifilm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.aifilm.support.IntegrationTest;

// 홈 API를 실제 MySQL까지 붙여 호출해 응답 구성과, 영화·배너가 늘어도 조회 쿼리 수가 그대로인지 확인한다.
// app은 JPA에 컴파일 의존이 없어서 테스트 데이터는 JDBC로 넣고, 쿼리 수는 MySQL이 실제로 받은 SELECT 수(Com_select)로 센다.
// 컨테이너를 다른 테스트와 함께 쓰므로 영상 ID에 고유 접두사를 붙이고 테스트가 끝나면 지운다.
@IntegrationTest
@AutoConfigureMockMvc
class HomeApiIntegrationTest {

	private static final Logger log = LoggerFactory.getLogger(HomeApiIntegrationTest.class);

	private static final String HOME = "/api/v1/home";
	private static final String VIDEO_ID_PREFIX = "home-it-";
	private static final LocalDateTime APPROVED_BASE = LocalDateTime.of(2026, 10, 1, 12, 0);
	// 같은 MySQL을 쓰는 다른 테스트 컨텍스트의 커넥션 초기화 쿼리가 섞일 수 있어서 여러 번 재고 가장 작은 값을 쓴다
	private static final int MEASURE_TIMES = 3;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private DataSource dataSource;

	// 테스트 안에서 붙인 영화 번호 → DB가 만든 영화 ID
	private final Map<Integer, Long> movieIds = new HashMap<>();

	@AfterEach
	void deleteTestData() throws SQLException {
		String testMovieIds = "SELECT id FROM movie WHERE youtube_video_id LIKE '" + VIDEO_ID_PREFIX + "%'";
		try (Connection connection = dataSource.getConnection();
			Statement statement = connection.createStatement()) {
			statement.executeUpdate("DELETE FROM home_banner_movie WHERE movie_id IN (" + testMovieIds + ")");
			statement.executeUpdate("DELETE FROM movie_genre WHERE movie_id IN (" + testMovieIds + ")");
			statement.executeUpdate("DELETE FROM movie WHERE youtube_video_id LIKE '" + VIDEO_ID_PREFIX + "%'");
		}
	}

	@Test
	@DisplayName("홈 조회는 노출 순서대로 배너를, 섹션마다 10편과 더보기 여부를 응답한다")
	void getsHome() throws Exception {
		// 1번이 가장 먼저 승인됐지만 평점이 가장 높다. 나머지 10편은 평점이 같아서 최근 승인 순서로 정렬된다.
		insertMovie(1, "4.90", 1L);
		for (int number = 2; number <= 11; number++) {
			insertMovie(number, "3.00", 1L);
		}
		addGenre(5, 3L);
		insertBanner(2, 2);
		insertBanner(5, 1);

		mockMvc.perform(get(HOME))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.code").value("SUCCESS"),
				jsonPath("$.data.banners.length()").value(2),
				jsonPath("$.data.banners[0].movieId").value(movieIds.get(5)),
				jsonPath("$.data.banners[0].genres[*].name").value(contains("SF", "드라마")),
				jsonPath("$.data.banners[1].movieId").value(movieIds.get(2)),
				jsonPath("$.data.genres.length()").value(7),
				jsonPath("$.data.sections[0].sectionType").value("LATEST"),
				jsonPath("$.data.sections[0].hasMore").value(true),
				jsonPath("$.data.sections[0].movies.length()").value(10),
				jsonPath("$.data.sections[0].movies[0].movieId").value(movieIds.get(11)),
				jsonPath("$.data.sections[0].movies[0].rankPosition").value(nullValue()),
				jsonPath("$.data.sections[1].sectionType").value("RATING_TOP"),
				jsonPath("$.data.sections[1].hasMore").value(true),
				jsonPath("$.data.sections[1].movies[0].movieId").value(movieIds.get(1)),
				jsonPath("$.data.sections[1].movies[0].rankPosition").value(1),
				jsonPath("$.data.sections[1].movies[1].movieId").value(movieIds.get(11)),
				jsonPath("$.data.sections[1].movies[1].rankPosition").value(2));
	}

	@Test
	@DisplayName("영화와 배너가 늘어나도 홈 조회 한 번에 나가는 SELECT 수는 같다")
	void selectCountDoesNotGrowWithMovies() throws Exception {
		for (int number = 1; number <= 3; number++) {
			insertMovie(number, "4.00", 1L, 3L);
		}
		insertBanner(1, 1);
		long withFewMovies = measureSelects();

		for (int number = 4; number <= 30; number++) {
			insertMovie(number, "4.00", 1L, 3L);
		}
		insertBanner(2, 2);
		insertBanner(3, 3);
		long withManyMovies = measureSelects();

		log.info("홈 조회 1회당 SELECT 수: 영화 3편·배너 1개 {}번, 영화 30편·배너 3개 {}번 (카운터 조회 보정 {}번 포함)",
			withFewMovies, withManyMovies, countingOverhead());
		// 배너, 배너 장르, 장르 메뉴, 최신 등록작, 평점 TOP을 쿼리 한 번씩으로 조회한다
		assertThat(withFewMovies).isEqualTo(5);
		assertThat(withManyMovies).isEqualTo(withFewMovies);
	}

	private long measureSelects() throws Exception {
		// 첫 호출의 준비 작업이 섞이지 않게 한 번 먼저 호출한다
		mockMvc.perform(get(HOME)).andExpect(status().isOk());
		long fewest = Long.MAX_VALUE;
		for (int i = 0; i < MEASURE_TIMES; i++) {
			long before = selectCount();
			mockMvc.perform(get(HOME)).andExpect(status().isOk());
			fewest = Math.min(fewest, selectCount() - before);
		}
		return fewest;
	}

	// 카운터를 읽는 문장 자체가 세어지는지 확인하려고, 요청 없이 두 번 읽은 차이를 로그에 함께 남긴다
	private long countingOverhead() throws SQLException {
		long before = selectCount();
		return selectCount() - before;
	}

	private long selectCount() throws SQLException {
		try (Connection connection = dataSource.getConnection();
			Statement statement = connection.createStatement();
			ResultSet rows = statement.executeQuery("SHOW GLOBAL STATUS LIKE 'Com_select'")) {
			rows.next();
			return rows.getLong("Value");
		}
	}

	private void insertMovie(int number, String averageRating, Long... genreIds) throws SQLException {
		try (Connection connection = dataSource.getConnection();
			PreparedStatement insert = connection.prepareStatement("""
				INSERT INTO movie (youtube_video_id, youtube_channel_id, youtube_channel_title, thumbnail_url, title,
				                   short_description, running_time_seconds, approved_at, average_rating)
				VALUES (?, 'channel-1', 'AI 스튜디오', 'https://example.com/thumbnail.jpg', ?, '짧은 소개', 600, ?, ?)
				""", Statement.RETURN_GENERATED_KEYS)) {
			insert.setString(1, VIDEO_ID_PREFIX + number);
			insert.setString(2, "홈 테스트 " + number);
			insert.setTimestamp(3, Timestamp.valueOf(APPROVED_BASE.plusMinutes(number)));
			insert.setBigDecimal(4, new BigDecimal(averageRating));
			insert.executeUpdate();
			try (ResultSet keys = insert.getGeneratedKeys()) {
				keys.next();
				movieIds.put(number, keys.getLong(1));
			}
		}
		addGenre(number, genreIds);
	}

	private void addGenre(int movieNumber, Long... genreIds) throws SQLException {
		try (Connection connection = dataSource.getConnection();
			PreparedStatement insert = connection.prepareStatement(
				"INSERT INTO movie_genre (movie_id, genre_id) VALUES (?, ?)")) {
			for (Long genreId : genreIds) {
				insert.setLong(1, movieIds.get(movieNumber));
				insert.setLong(2, genreId);
				insert.executeUpdate();
			}
		}
	}

	private void insertBanner(int movieNumber, int displayOrder) throws SQLException {
		try (Connection connection = dataSource.getConnection();
			PreparedStatement insert = connection.prepareStatement(
				"INSERT INTO home_banner_movie (movie_id, display_order) VALUES (?, ?)")) {
			insert.setLong(1, movieIds.get(movieNumber));
			insert.setInt(2, displayOrder);
			insert.executeUpdate();
		}
	}

}
