package com.aifilm.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.mysql.MySQLContainer;

// app 통합 테스트가 함께 쓰는 MySQL 컨테이너 선언. @IntegrationTest가 @ImportTestcontainers로 가져간다.
// static 필드라 JVM에 하나만 만들어져, 실행 모드별로 테스트 컨텍스트가 여러 개 떠도 같은 컨테이너를 쓴다.
// 이미지·문자셋·타임존은 docker-compose.yml과 application.yml의 로컬 설정에 맞춘다.
public final class MySqlTestContainer {

	@ServiceConnection
	static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4")
		.withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci")
		.withEnv("TZ", "Asia/Seoul")
		.withUrlParam("serverTimezone", "Asia/Seoul")
		.withUrlParam("characterEncoding", "UTF-8");

	private MySqlTestContainer() {
	}

}
