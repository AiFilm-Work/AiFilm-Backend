package com.aifilm.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.core.annotation.AliasFor;

// app 통합 테스트용 어노테이션. 앱 전체 컨텍스트를 띄우고 DataSource를 테스트 MySQL 컨테이너에 연결한다.
// 프로파일별 설정 파일(application-local.yml 등)의 접속 정보 대신 @ServiceConnection이 컨테이너 주소를 넣어 준다.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@ImportTestcontainers(MySqlTestContainer.class)
public @interface IntegrationTest {

	// 기본은 MockMvc로 확인하는 MOCK 환경이다. Tomcat 밸브처럼 실제 서버에서만 동작하는 기능은 RANDOM_PORT로 띄운다.
	@AliasFor(annotation = SpringBootTest.class, attribute = "webEnvironment")
	SpringBootTest.WebEnvironment webEnvironment() default SpringBootTest.WebEnvironment.MOCK;

}
