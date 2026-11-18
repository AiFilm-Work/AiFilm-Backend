package com.aifilm.api.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// 허용한 프론트 출처에서만 브라우저로 API를 호출할 수 있게 한다. 같은 출처(Swagger UI)와 브라우저가 아닌 클라이언트(앱)는 영향이 없다.
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CorsProperties.class)
@RequiredArgsConstructor
public class CorsConfig implements WebMvcConfigurer {

    private static final long PREFLIGHT_CACHE_SECONDS = 3600;

    private final CorsProperties corsProperties;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 인증은 Authorization 헤더로 할 예정이라 쿠키(credentials)는 허용하지 않는다
        registry.addMapping("/**")
                .allowedOrigins(corsProperties.allowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE")
                .allowedHeaders("*")
                .maxAge(PREFLIGHT_CACHE_SECONDS);
    }

}
