package com.aifilm.infra.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;

// 리포지토리 테스트용 어노테이션. JPA 관련 빈만 띄우고 테스트 MySQL 컨테이너에 Flyway migration을 적용한다.
// @ServiceConnection으로 연결한 컨테이너는 임베디드 DB로 바뀌지 않는다. (@AutoConfigureTestDatabase 기본값 NON_TEST)
// 운영과 같이 ddl-auto=validate로 띄워 엔티티와 migration이 다르면 컨텍스트가 뜨지 않게 한다.
// 각 테스트는 트랜잭션 안에서 실행되고 끝나면 롤백된다.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@ImportTestcontainers(MySqlTestContainer.class)
public @interface RepositoryTest {
}
