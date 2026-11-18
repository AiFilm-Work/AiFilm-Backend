package com.aifilm.api.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.util.DigestUtils;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
public class SwaggerUiConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // swagger-ui.html이 화면 리소스와 스펙을 /static/swagger-ui/ 경로로 불러온다.
        revalidateByContent(registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/"));
        revalidateByContent(registry.addResourceHandler("/swagger-ui.html")
                .addResourceLocations("classpath:/static/swagger-ui/"));
    }

    // 스펙은 배포마다 바뀌므로 브라우저가 매번 서버에 확인하게 한다(no-cache).
    // jar 안 파일의 시각은 빌드 때 1980-02-01로 고정되어 Last-Modified로는 변경을 알 수 없으므로, 파일 내용으로 만든 ETag로 비교한다.
    private static void revalidateByContent(ResourceHandlerRegistration registration) {
        registration.setCacheControl(CacheControl.noCache())
                .setUseLastModified(false)
                .setEtagGenerator(SwaggerUiConfig::contentHash);
    }

    // 요청마다 계산한다. 문서 화면 요청은 적고, 로컬에서 앱을 띄운 채 스펙을 다시 만들어도 바로 반영된다.
    private static String contentHash(Resource resource) {
        try (InputStream content = resource.getInputStream()) {
            return DigestUtils.md5DigestAsHex(content);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

}
