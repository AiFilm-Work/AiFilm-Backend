package com.aifilm;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.aifilm.support.IntegrationTest;

// spring.jpa.open-in-view=false로 요청 동안 영속성 컨텍스트를 열어두는 인터셉터가 등록되지 않는지 확인한다.
// app은 JPA에 컴파일 의존이 없어서 Spring Boot가 등록하는 빈 이름으로 확인한다.
@IntegrationTest
class OpenInViewTest {

	@Autowired
	private ApplicationContext context;

	@Test
	@DisplayName("open-in-view를 끄면 OSIV 인터셉터가 등록되지 않는다")
	void openInViewInterceptorNotRegistered() {
		assertThat(context.containsBean("openEntityManagerInViewInterceptor")).isFalse();
	}

}
