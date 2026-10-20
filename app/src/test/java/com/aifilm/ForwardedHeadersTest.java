package com.aifilm;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aifilm.support.IntegrationTest;

// prod 프로파일에서 Nginx가 넘기는 X-Forwarded-* 헤더를 읽는지 확인한다.
// RemoteIpValve는 MockMvc가 아니라 실제 Tomcat에서만 동작하므로 실제 포트로 띄워 HTTP 요청을 보낸다.
@IntegrationTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("prod")
@Import(ForwardedHeadersTest.RequestInfoController.class)
class ForwardedHeadersTest {

	private final HttpClient httpClient = HttpClient.newHttpClient();

	@Value("${local.server.port}")
	private int port;

	@Test
	@DisplayName("프록시가 넘긴 X-Forwarded-Proto와 X-Forwarded-For를 scheme과 클라이언트 IP로 읽는다")
	void readsForwardedHeaders() throws Exception {
		HttpRequest request = requestBuilder()
			.header("X-Forwarded-Proto", "https")
			.header("X-Forwarded-For", "203.0.113.10")
			.build();

		assertThat(send(request)).isEqualTo("https 203.0.113.10");
	}

	@Test
	@DisplayName("헤더가 없으면 실제 연결의 scheme과 IP를 쓴다")
	void withoutForwardedHeaders() throws Exception {
		assertThat(send(requestBuilder().build())).isEqualTo("http 127.0.0.1");
	}

	private HttpRequest.Builder requestBuilder() {
		return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/test/request-info"));
	}

	private String send(HttpRequest request) throws Exception {
		return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
	}

	// 이 테스트 컨텍스트에만 등록되는 확인용 엔드포인트
	@RestController
	static class RequestInfoController {

		@GetMapping("/test/request-info")
		String requestInfo(HttpServletRequest request) {
			return request.getScheme() + " " + request.getRemoteAddr();
		}

	}

}
