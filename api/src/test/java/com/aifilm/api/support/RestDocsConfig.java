package com.aifilm.api.support;

import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;

import org.springframework.boot.restdocs.test.autoconfigure.RestDocsMockMvcConfigurationCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

// Swagger UI는 요청/응답 예시를 문자열 그대로 보여주므로 JSON을 줄바꿈해서 남긴다.
@TestConfiguration(proxyBeanMethods = false)
class RestDocsConfig {

	@Bean
	RestDocsMockMvcConfigurationCustomizer prettyPrintCustomizer() {
		return configurer -> configurer.operationPreprocessors()
			.withRequestDefaults(prettyPrint())
			.withResponseDefaults(prettyPrint());
	}

}
