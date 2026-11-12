package com.aifilm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.aifilm.support.IntegrationTest;
import com.aifilm.support.SelectCounter;

// 영화 상세 API를 실제 MySQL까지 붙여 호출해 응답 형식, 404 구분, 조회 쿼리 수를 확인한다.
// 컨테이너를 다른 테스트와 함께 쓰므로 영상 ID에 고유 접두사를 붙이고 테스트가 끝나면 지운다.
@IntegrationTest
@AutoConfigureMockMvc
class MovieApiIntegrationTest {

	private static final String MOVIE = "/api/v1/movies/{movieId}";
	private static final String VIDEO_ID_PREFIX = "movie-it-";
	// DB에 마이크로초까지 저장된 값이 응답에서 초까지만 나가는지 보려고 소수점 아래 값을 넣는다
	private static final LocalDateTime PUBLISHED_AT = LocalDateTime.of(2025, 1, 10, 13, 20, 0, 123_456_000);
	private static final LocalDateTime APPROVED_AT = LocalDateTime.of(2026, 10, 1, 12, 0, 0, 654_321_000);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private DataSource dataSource;

	@AfterEach
	void deleteTestData() throws SQLException {
		String testMovieIds = "SELECT id FROM movie WHERE youtube_video_id LIKE '" + VIDEO_ID_PREFIX + "%'";
		try (Connection connection = dataSource.getConnection();
			Statement statement = connection.createStatement()) {
			statement.executeUpdate("DELETE FROM movie_genre WHERE movie_id IN (" + testMovieIds + ")");
			statement.executeUpdate("DELETE FROM movie WHERE youtube_video_id LIKE '" + VIDEO_ID_PREFIX + "%'");
		}
	}

	@Test
	@DisplayName("영화 상세는 장르, 통계와 함께 시각을 초까지 응답한다")
	void getsMovieDetail() throws Exception {
		long movieId = insertMovie("detail", "ACTIVE", "AVAILABLE", 1L, 3L);

		mockMvc.perform(get(MOVIE, movieId))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.code").value("SUCCESS"),
				jsonPath("$.data.movieId").value(movieId),
				jsonPath("$.data.youtubeVideoId").value(VIDEO_ID_PREFIX + "detail"),
				jsonPath("$.data.youtubeDescription").value("YouTube 원본 설명"),
				jsonPath("$.data.youtubePublishedAt").value("2025-01-10T13:20:00"),
				jsonPath("$.data.approvedAt").value("2026-10-01T12:00:00"),
				jsonPath("$.data.genres[*].name").value(contains("SF", "드라마")),
				jsonPath("$.data.statistics.totalViewCount").value(0),
				// 승인 후 7일이 지난 영화라 NEW 배지가 없다
				jsonPath("$.data.badges").isEmpty());
	}

	@ParameterizedTest
	@CsvSource({"HIDDEN, AVAILABLE", "ACTIVE, UNAVAILABLE"})
	@DisplayName("숨김 처리됐거나 재생할 수 없는 영화는 404와 MOVIE_404_2로 응답한다")
	void respondsUnavailable(String screeningStatus, String availabilityStatus) throws Exception {
		long movieId = insertMovie("unavailable", screeningStatus, availabilityStatus, 1L);

		mockMvc.perform(get(MOVIE, movieId))
			.andExpectAll(status().isNotFound(), jsonPath("$.code").value("MOVIE_404_2"));
	}

	@Test
	@DisplayName("없는 영화는 404와 MOVIE_404_1로 응답한다")
	void respondsNotFound() throws Exception {
		mockMvc.perform(get(MOVIE, Long.MAX_VALUE))
			.andExpectAll(status().isNotFound(), jsonPath("$.code").value("MOVIE_404_1"));
	}

	@Test
	@DisplayName("영화 상세 조회 한 번에 나가는 SELECT는 영화 1번, 장르 1번이다")
	void selectsMovieAndGenresOnce() throws Exception {
		long movieId = insertMovie("select-count", "ACTIVE", "AVAILABLE", 1L, 3L, 5L);

		long selects = new SelectCounter(dataSource)
			.measure(() -> mockMvc.perform(get(MOVIE, movieId)).andExpect(status().isOk()));

		assertThat(selects).isEqualTo(2);
	}

	private long insertMovie(String videoId, String screeningStatus, String availabilityStatus, Long... genreIds)
		throws SQLException {
		long movieId;
		try (Connection connection = dataSource.getConnection();
			PreparedStatement insert = connection.prepareStatement("""
				INSERT INTO movie (youtube_video_id, youtube_channel_id, youtube_channel_title, thumbnail_url, title,
				                   youtube_description, short_description, running_time_seconds, youtube_published_at,
				                   approved_at, screening_status, availability_status)
				VALUES (?, 'channel-1', 'AI 스튜디오', 'https://example.com/thumbnail.jpg', '상세 테스트',
				        'YouTube 원본 설명', '짧은 소개', 600, ?, ?, ?, ?)
				""", Statement.RETURN_GENERATED_KEYS)) {
			insert.setString(1, VIDEO_ID_PREFIX + videoId);
			insert.setTimestamp(2, Timestamp.valueOf(PUBLISHED_AT));
			insert.setTimestamp(3, Timestamp.valueOf(APPROVED_AT));
			insert.setString(4, screeningStatus);
			insert.setString(5, availabilityStatus);
			insert.executeUpdate();
			try (ResultSet keys = insert.getGeneratedKeys()) {
				keys.next();
				movieId = keys.getLong(1);
			}
		}
		try (Connection connection = dataSource.getConnection();
			PreparedStatement insert = connection.prepareStatement(
				"INSERT INTO movie_genre (movie_id, genre_id) VALUES (?, ?)")) {
			for (Long genreId : genreIds) {
				insert.setLong(1, movieId);
				insert.setLong(2, genreId);
				insert.executeUpdate();
			}
		}
		return movieId;
	}

}
