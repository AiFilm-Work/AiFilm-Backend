package com.aifilm.api;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;

import jakarta.persistence.EntityManagerFactory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;

// api 모듈의 @WebMvcTest가 테스트 전용 설정(ApiTestApplication)으로 뜨는지 검증한다.
// 컨트롤러 문서화 테스트의 기반이므로, DB(DataSource, JPA) 없이 웹 계층만 올라오는지도 함께 확인한다.
// 확인 대상은 테스트 설정과 자동 설정이라 컨트롤러는 올리지 않는다. (컨트롤러는 각 테스트가 controllers로 지정한다)
@WebMvcTest(useDefaultFilters = false)
class ApiWebMvcTestConfigurationTest {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("테스트 전용 설정으로 웹 계층 슬라이스가 뜨고 MockMvc를 사용할 수 있다")
	void webMvcSliceLoadsWithTestConfiguration() {
		assertThat(context.getBeanNamesForType(ApiTestApplication.class)).isNotEmpty();
		assertThat(mockMvc).isNotNull();
	}

	@Test
	@DisplayName("DB 관련 빈(DataSource, JPA) 없이 뜬다")
	void webMvcSliceLoadsWithoutDatabase() {
		assertThat(context.getBeanNamesForType(DataSource.class)).isEmpty();
		assertThat(context.getBeanNamesForType(EntityManagerFactory.class)).isEmpty();
	}

}
