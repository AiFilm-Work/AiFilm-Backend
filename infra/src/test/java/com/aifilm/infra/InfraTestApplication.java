package com.aifilm.infra;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;

// infra 모듈 테스트 전용 설정. main 클래스가 app 모듈에 있어서 @DataJpaTest가 찾을 @SpringBootConfiguration을 둔다.
// 리포지토리는 이 패키지(com.aifilm.infra) 아래에서 찾고, 엔티티는 domain 모듈에 있어서 스캔 범위를 따로 지정한다.
@SpringBootApplication
@EntityScan(basePackages = "com.aifilm.domain")
class InfraTestApplication {
}
