package com.aifilm;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.aifilm.support.IntegrationTest;

// 앱이 뜰 때 Flyway가 실행되어 테스트 MySQL 컨테이너에 스키마 이력 테이블을 만드는지 확인한다.
// app은 infra에 컴파일 의존이 없으므로 Flyway API 대신 JDBC 메타데이터로 확인한다.
@IntegrationTest
class FlywayMigrationTest {

	@Autowired
	private DataSource dataSource;

	@Test
	@DisplayName("앱이 뜨면 Flyway가 실행되어 flyway_schema_history 테이블이 생긴다")
	void createsSchemaHistoryTable() throws SQLException {
		try (Connection connection = dataSource.getConnection();
			ResultSet tables = connection.getMetaData()
				.getTables(connection.getCatalog(), null, "flyway_schema_history", new String[] {"TABLE"})) {
			assertThat(tables.next()).isTrue();
		}
	}
}
