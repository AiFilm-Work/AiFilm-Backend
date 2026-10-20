package com.aifilm.infra.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.mysql.MySQLContainer;

// infra 리포지토리 테스트가 함께 쓰는 MySQL 컨테이너. app 모듈의 MySqlTestContainer와 같은 설정이다.
// static 필드라 JVM에 하나만 만들어져 여러 테스트 클래스가 같은 컨테이너를 쓴다.
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
