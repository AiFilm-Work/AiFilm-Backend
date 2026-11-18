package com.aifilm.api.home;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aifilm.api.genre.dto.GenreResponse;
import com.aifilm.api.home.dto.HomeBannerResponse;
import com.aifilm.api.home.dto.HomeMovieResponse;
import com.aifilm.api.home.dto.HomeResponse;
import com.aifilm.api.home.dto.HomeSectionResponse;
import com.aifilm.api.home.dto.HomeSectionType;
import com.aifilm.api.support.ControllerTest;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// 입력값이 없고 로그인도 필요 없는 API라 4xx 실패 응답이 없어서 성공 케이스만 문서화한다.
@ControllerTest(controllers = HomeController.class)
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HomeService homeService;

    @Test
    @DisplayName("홈을 조회하면 200과 배너, 장르 메뉴, 영화 섹션을 반환한다")
    void getHome() throws Exception {
        given(homeService.getHome()).willReturn(sampleHome());

        mockMvc.perform(get("/api/v1/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.banners[0].youtubeVideoId").value("abc123"))
                .andExpect(jsonPath("$.data.sections[0].sectionType").value("LATEST"))
                .andExpect(jsonPath("$.data.sections[0].movies[0].rankPosition").value(nullValue()))
                .andExpect(jsonPath("$.data.sections[1].sectionType").value("RATING_TOP"))
                .andExpect(jsonPath("$.data.sections[1].movies[0].rankPosition").value(1))
                .andDo(document("home-get",
                        resource(ResourceSnippetParameters.builder()
                                .tag("홈")
                                .summary("홈 화면 조회")
                                .description("""
                                        홈 화면의 배너, 장르 메뉴, 영화 섹션(최신 등록작, 평점 TOP 10)을 한 번에 조회합니다.
                                        노출 중(ACTIVE)이고 YouTube에서 재생할 수 있는(AVAILABLE) 영화만 내려갑니다.
                                        섹션은 10편씩이고, 뒤에 영화가 더 있으면 hasMore가 true입니다. 로그인이 필요 없습니다.""")
                                .responseFields(
                                        fieldWithPath("status").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                        fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                        fieldWithPath("data.banners[].bannerId").type(JsonFieldType.NUMBER)
                                                .description("배너 ID"),
                                        fieldWithPath("data.banners[].movieId").type(JsonFieldType.NUMBER)
                                                .description("영화 ID"),
                                        fieldWithPath("data.banners[].title").type(JsonFieldType.STRING)
                                                .description("영화 제목"),
                                        fieldWithPath("data.banners[].description").type(JsonFieldType.STRING)
                                                .description("영화 짧은 소개"),
                                        fieldWithPath("data.banners[].thumbnailUrl").type(JsonFieldType.STRING)
                                                .description("배너 이미지 URL (영화 썸네일)"),
                                        fieldWithPath("data.banners[].youtubeVideoId").type(JsonFieldType.STRING)
                                                .description("미리보기로 재생할 YouTube 영상 ID"),
                                        fieldWithPath("data.banners[].averageRating").type(JsonFieldType.NUMBER)
                                                .description("평균 평점 (0.00 ~ 5.00)"),
                                        fieldWithPath("data.banners[].reviewCount").type(JsonFieldType.NUMBER)
                                                .description("리뷰 수"),
                                        fieldWithPath("data.banners[].genres[].genreId").type(JsonFieldType.NUMBER)
                                                .description("영화 장르 ID"),
                                        fieldWithPath("data.banners[].genres[].name").type(JsonFieldType.STRING)
                                                .description("영화 장르 이름"),
                                        fieldWithPath("data.banners[].startSeconds").type(JsonFieldType.NUMBER)
                                                .description("미리보기 시작 위치(초)"),
                                        fieldWithPath("data.banners[].previewDurationSeconds").type(
                                                        JsonFieldType.NUMBER)
                                                .description("미리보기 재생 길이(초)"),
                                        fieldWithPath("data.genres[].genreId").type(JsonFieldType.NUMBER)
                                                .description("장르 ID"),
                                        fieldWithPath("data.genres[].name").type(JsonFieldType.STRING)
                                                .description("장르 이름"),
                                        fieldWithPath("data.sections[].sectionType").type(JsonFieldType.STRING)
                                                .description("섹션 종류. LATEST(최신 등록작), RATING_TOP(평점 TOP 10) 순서로 내려갑니다."),
                                        fieldWithPath("data.sections[].title").type(JsonFieldType.STRING)
                                                .description("화면에 보여줄 섹션 제목"),
                                        fieldWithPath("data.sections[].genreId").type(JsonFieldType.NUMBER).optional()
                                                .description("장르 기반 섹션의 장르 ID. 지금 섹션들은 null입니다."),
                                        fieldWithPath("data.sections[].hasMore").type(JsonFieldType.BOOLEAN)
                                                .description("보여준 10편 뒤에 노출 가능한 영화가 더 있는지 여부"),
                                        fieldWithPath("data.sections[].movies[].movieId").type(JsonFieldType.NUMBER)
                                                .description("영화 ID"),
                                        fieldWithPath("data.sections[].movies[].title").type(JsonFieldType.STRING)
                                                .description("영화 제목"),
                                        fieldWithPath("data.sections[].movies[].thumbnailUrl").type(
                                                        JsonFieldType.STRING)
                                                .description("썸네일 URL"),
                                        fieldWithPath("data.sections[].movies[].averageRating").type(
                                                        JsonFieldType.NUMBER)
                                                .description("평균 평점 (0.00 ~ 5.00)"),
                                        fieldWithPath("data.sections[].movies[].reviewCount").type(JsonFieldType.NUMBER)
                                                .description("리뷰 수"),
                                        fieldWithPath("data.sections[].movies[].rankPosition").type(
                                                        JsonFieldType.NUMBER).optional()
                                                .description("순위. RATING_TOP 섹션에서만 1부터 채우고 그 외 섹션은 null입니다."))
                                .build())));
    }

    private static HomeResponse sampleHome() {
        GenreResponse sf = new GenreResponse(1L, "SF");
        GenreResponse drama = new GenreResponse(3L, "드라마");
        HomeBannerResponse banner = new HomeBannerResponse(1L, 101L, "네온 메모리", "기억은 사라져도, 사랑은 남는다.",
                "https://example.com/neon-memory.jpg", "abc123", new BigDecimal("4.70"), 21, List.of(sf, drama), 20,
                10);
        HomeSectionResponse latest = new HomeSectionResponse(HomeSectionType.LATEST, "최신 등록작", null, true,
                List.of(new HomeMovieResponse(301L, "오로라", "https://example.com/aurora.jpg",
                        new BigDecimal("4.60"), 12, null)));
        HomeSectionResponse ratingTop = new HomeSectionResponse(HomeSectionType.RATING_TOP, "평점 TOP 10", null, true,
                List.of(new HomeMovieResponse(201L, "첫 겨울꽃", "https://example.com/first-winter.jpg",
                        new BigDecimal("4.80"), 73, 1)));
        return new HomeResponse(List.of(banner), List.of(sf, new GenreResponse(2L, "공포"), drama),
                List.of(latest, ratingTop));
    }

}
