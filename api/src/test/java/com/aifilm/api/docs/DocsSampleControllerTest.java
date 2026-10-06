package com.aifilm.api.docs;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.MockMvc;

import com.epages.restdocs.apispec.ParameterDescriptorWithType;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.SimpleType;

import com.aifilm.api.support.ControllerTest;

@ControllerTest(controllers = DocsSampleController.class)
class DocsSampleControllerTest {

	private static final String TAG = "샘플";
	private static final String SUMMARY = "샘플 조회";
	private static final String DESCRIPTION = "API 문서화 파이프라인 확인용 샘플 API입니다. 첫 실제 API를 문서화할 때 삭제합니다.";

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("샘플을 조회하면 200과 샘플 정보를 반환한다")
	void getSample() throws Exception {
		// 경로 변수를 문서화하려면 RestDocumentationRequestBuilders로 요청을 만들어야 한다.
		mockMvc.perform(get("/api/v1/docs-samples/{sampleId}", 1L))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.sampleId").value(1))
			.andDo(document("docs-sample-get",
				resource(ResourceSnippetParameters.builder()
					.tag(TAG)
					.summary(SUMMARY)
					.description(DESCRIPTION)
					.pathParameters(sampleIdParameter())
					.responseFields(
						fieldWithPath("status").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
						fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
						fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
						fieldWithPath("data.sampleId").type(JsonFieldType.NUMBER).description("샘플 ID"),
						fieldWithPath("data.name").type(JsonFieldType.STRING).description("샘플 이름"))
					.build())));
	}

	@Test
	@DisplayName("없는 샘플을 조회하면 404와 실패 응답을 반환한다")
	void getSampleNotFound() throws Exception {
		mockMvc.perform(get("/api/v1/docs-samples/{sampleId}", 999L))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("SAMPLE_404_1"))
			.andDo(document("docs-sample-get-not-found",
				resource(ResourceSnippetParameters.builder()
					.tag(TAG)
					.summary(SUMMARY)
					.description(DESCRIPTION)
					.pathParameters(sampleIdParameter())
					.responseFields(
						fieldWithPath("status").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
						fieldWithPath("code").type(JsonFieldType.STRING).description("에러 코드"),
						fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지"),
						fieldWithPath("data").type(JsonFieldType.NULL).description("실패 응답에서는 null"))
					.build())));
	}

	private ParameterDescriptorWithType sampleIdParameter() {
		return parameterWithName("sampleId").description("샘플 ID").type(SimpleType.INTEGER);
	}

}
