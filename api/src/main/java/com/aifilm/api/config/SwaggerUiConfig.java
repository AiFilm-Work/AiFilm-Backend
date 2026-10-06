package com.aifilm.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
public class SwaggerUiConfig implements WebMvcConfigurer {

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		// swagger-ui.html이 화면 리소스와 스펙을 /static/swagger-ui/ 경로로 불러온다.
		registry.addResourceHandler("/static/**")
			.addResourceLocations("classpath:/static/");
		registry.addResourceHandler("/swagger-ui.html")
			.addResourceLocations("classpath:/static/swagger-ui/");
	}

}
