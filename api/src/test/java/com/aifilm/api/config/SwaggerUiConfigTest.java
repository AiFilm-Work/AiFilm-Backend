package com.aifilm.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

// 컨트롤러 없이 Swagger UI 리소스 설정만 올린다. 컨트롤러가 늘어나도 이 테스트는 영향을 받지 않는다.
@WebMvcTest(useDefaultFilters = false)
@Import(SwaggerUiConfig.class)
class SwaggerUiConfigTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("/swagger-ui.html에서 Swagger UI 페이지를 연다")
	void swaggerUiPage() throws Exception {
		String page = mockMvc.perform(get("/swagger-ui.html"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
			.andReturn()
			.getResponse()
			.getContentAsString(StandardCharsets.UTF_8);

		assertThat(page).contains("/static/swagger-ui/swagger-ui-bundle.js");
	}

	@Test
	@DisplayName("화면 리소스를 /static/swagger-ui/ 경로로 제공한다")
	void swaggerUiResources() throws Exception {
		mockMvc.perform(get("/static/swagger-ui/swagger-ui-bundle.js"))
			.andExpect(status().isOk());
		mockMvc.perform(get("/static/swagger-ui/swagger-ui.css"))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("Swagger UI가 빌드 때 생성된 스펙을 읽도록 설정되어 있다")
	void swaggerUiReadsGeneratedSpec() throws Exception {
		String initializer = mockMvc.perform(get("/static/swagger-ui/swagger-initializer.js"))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString(StandardCharsets.UTF_8);

		assertThat(initializer).contains("url: \"/static/swagger-ui/openapi3.yaml\"");
	}

}
