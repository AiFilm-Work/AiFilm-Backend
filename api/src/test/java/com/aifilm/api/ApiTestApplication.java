package com.aifilm.api;

import org.springframework.boot.autoconfigure.SpringBootApplication;

// api 모듈 테스트 전용 설정. main 클래스가 app 모듈로 옮겨가서, @WebMvcTest 같은 테스트가 찾을 @SpringBootConfiguration을 둔다.
// 테스트 소스에만 있으므로 운영 jar와 app 모듈에는 포함되지 않는다.
@SpringBootApplication
class ApiTestApplication {
}
