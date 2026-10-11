package com.aifilm.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;

// app 통합 테스트용 어노테이션. 앱 전체 컨텍스트를 띄우고 DataSource를 테스트 MySQL 컨테이너에 연결한다.
// application.yml의 접속 정보(localhost, DB_* 환경변수) 대신 @ServiceConnection이 컨테이너 주소를 넣어 준다.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@ImportTestcontainers(MySqlTestContainer.class)
public @interface IntegrationTest {
}
