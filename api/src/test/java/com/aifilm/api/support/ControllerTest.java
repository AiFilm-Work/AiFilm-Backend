package com.aifilm.api.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@WebMvcTest
@AutoConfigureRestDocs
// 인증 같은 서블릿 필터는 문서화 대상이 아니므로 MockMvc에 추가하지 않는다.
@AutoConfigureMockMvc(addFilters = false)
@Import(RestDocsConfig.class)
public @interface ControllerTest {

	// @AliasFor가 없으면 값을 넘겨도 무시되고 모든 컨트롤러가 슬라이스에 올라간다.
	@AliasFor(annotation = WebMvcTest.class, attribute = "controllers")
	Class<?>[] controllers() default {};

}
