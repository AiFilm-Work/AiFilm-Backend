package com.aifilm.infra.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

// JPA Entity / Repository 스캔 설정. Entity와 Repository가 여러 모듈에 걸쳐 있을 수 있어 com.aifilm 전체를 스캔한다.
// 메인 클래스에 두면 @WebMvcTest 같은 슬라이스 테스트에도 강제로 적용되어 컨텍스트 로딩이 실패하므로,
// 영속성 계층인 infra의 별도 @Configuration으로 분리한다. (슬라이스 테스트는 스캔으로 찾은 @Configuration을 제외한다)
@Configuration(proxyBeanMethods = false)
@EntityScan(basePackages = "com.aifilm")
@EnableJpaRepositories(basePackages = "com.aifilm")
public class JpaConfig {
}
