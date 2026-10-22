package com.aifilm.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.DigestUtils;

// 컨트롤러 없이 Swagger UI 리소스 설정만 올린다. 컨트롤러가 늘어나도 이 테스트는 영향을 받지 않는다.
@WebMvcTest(useDefaultFilters = false)
@Import(SwaggerUiConfig.class)
class SwaggerUiConfigTest {

	private static final String INITIALIZER = "/static/swagger-ui/swagger-initializer.js";

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

	@Test
	@DisplayName("Swagger UI 파일은 매번 서버에 확인하도록 no-cache와 ETag로 내려가고, 고정된 Last-Modified는 보내지 않는다")
	void swaggerUiFilesAreRevalidated() throws Exception {
		for (String path : new String[] {"/swagger-ui.html", INITIALIZER}) {
			mockMvc.perform(get(path))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-cache"))
				.andExpect(header().exists(HttpHeaders.ETAG))
				.andExpect(header().doesNotExist(HttpHeaders.LAST_MODIFIED));
		}
	}

	@Test
	@DisplayName("ETag는 파일 내용의 해시라서, 배포로 내용이 바뀌면 ETag도 바뀐다")
	void etagIsContentHash() throws Exception {
		String expected;
		try (InputStream content = new ClassPathResource("static/swagger-ui/swagger-initializer.js").getInputStream()) {
			expected = "\"" + DigestUtils.md5DigestAsHex(content) + "\"";
		}

		mockMvc.perform(get(INITIALIZER))
			.andExpect(header().string(HttpHeaders.ETAG, expected));
	}

	@Test
	@DisplayName("브라우저가 가진 ETag와 내용이 같으면 파일을 다시 보내지 않고 304로 응답한다")
	void notModifiedWhenEtagMatches() throws Exception {
		String etag = mockMvc.perform(get(INITIALIZER))
			.andReturn()
			.getResponse()
			.getHeader(HttpHeaders.ETAG);

		mockMvc.perform(get(INITIALIZER).header(HttpHeaders.IF_NONE_MATCH, etag))
			.andExpect(status().isNotModified());
	}

	@Test
	@DisplayName("If-Modified-Since만으로는 304를 주지 않는다")
	void ignoresIfModifiedSince() throws Exception {
		// 테스트에서는 파일 시각이 실제 시각이라, 먼 미래 시각을 보내 '그 뒤로 바뀌지 않았다'는 조건을 만든다.
		// 수정 전에는 이 요청이 304였다.
		mockMvc.perform(get(INITIALIZER).header(HttpHeaders.IF_MODIFIED_SINCE, "Fri, 31 Dec 2100 00:00:00 GMT"))
			.andExpect(status().isOk());
	}

}
