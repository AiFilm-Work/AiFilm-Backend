package com.aifilm.api.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentationConfigurer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RestController;

@ControllerTest(controllers = TestPingController.class)
class ControllerTestConfigurationTest {

	private static final String FILTER_HEADER = "X-Test-Filter";

	@Autowired
	private ApplicationContext context;

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("REST Docs 자동 설정이 적용된다")
	void restDocsAutoConfigured() {
		assertThat(context.getBeanNamesForType(MockMvcRestDocumentationConfigurer.class)).isNotEmpty();
	}

	@Test
	@DisplayName("controllers로 지정한 컨트롤러만 슬라이스에 올라간다")
	void loadsOnlySpecifiedControllers() {
		assertThat(context.getBeansWithAnnotation(RestController.class).values())
			.singleElement()
			.isInstanceOf(TestPingController.class);
	}

	@Test
	@DisplayName("서블릿 필터는 MockMvc 요청에 적용되지 않는다")
	void servletFiltersAreNotApplied() throws Exception {
		mockMvc.perform(get("/test/ping"))
			.andExpect(status().isOk())
			.andExpect(header().doesNotExist(FILTER_HEADER));
	}

	// 적용되면 응답에 헤더를 남기는 필터
	@TestConfiguration
	static class HeaderFilterConfig {

		@Bean
		Filter headerFilter() {
			return (request, response, chain) -> {
				((HttpServletResponse) response).setHeader(FILTER_HEADER, "applied");
				chain.doFilter(request, response);
			};
		}

	}

}
