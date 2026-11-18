package com.aifilm.api.movie;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aifilm.api.genre.dto.GenreResponse;
import com.aifilm.api.movie.dto.MovieBadgeResponse;
import com.aifilm.api.movie.dto.MovieBadgeType;
import com.aifilm.api.movie.dto.MovieDetailResponse;
import com.aifilm.api.movie.dto.MovieStatisticsResponse;
import com.aifilm.api.support.ControllerTest;
import com.aifilm.common.error.BusinessException;
import com.aifilm.domain.movie.MovieErrorCode;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.ResourceSnippetParametersBuilder;
import com.epages.restdocs.apispec.SimpleType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ControllerTest(controllers = MovieController.class)
class MovieControllerTest {

    private static final String MOVIE = "/api/v1/movies/{movieId}";
    // 같은 API의 성공·실패 문서가 스펙에서 하나로 합쳐지도록 태그, 요약, 설명을 함께 쓴다
    private static final String TAG = "영화";
    private static final String SUMMARY = "영화 상세 조회";
    private static final String DESCRIPTION = """
            영화 한 편의 상세 정보를 조회합니다. 로그인이 필요 없습니다.
            노출 중(ACTIVE)이고 YouTube에서 재생할 수 있는(AVAILABLE) 영화만 조회됩니다.
            없는 영화는 MOVIE_404_1, 숨김 처리됐거나 YouTube에서 재생할 수 없게 된 영화는 MOVIE_404_2로 응답합니다.
            시각은 KST 기준 yyyy-MM-dd'T'HH:mm:ss 형식입니다.""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MovieService movieService;

    @Test
    @DisplayName("영화를 조회하면 200과 상세 정보를 반환하고, 시각은 초까지만 내린다")
    void getMovieDetail() throws Exception {
        given(movieService.getMovieDetail(101L)).willReturn(sampleDetail());

        mockMvc.perform(get(MOVIE, 101L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.youtubeVideoId").value("abc123"))
                .andExpect(jsonPath("$.data.youtubePublishedAt").value("2025-01-10T13:20:00"))
                .andExpect(jsonPath("$.data.approvedAt").value("2026-10-20T12:00:00"))
                .andExpect(jsonPath("$.data.badges[0].type").value("NEW"))
                .andDo(document("movie-get", resource(parameters()
                        .responseFields(
                                fieldWithPath("status").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                fieldWithPath("data.movieId").type(JsonFieldType.NUMBER).description("영화 ID"),
                                fieldWithPath("data.title").type(JsonFieldType.STRING)
                                        .description("AI FILM에서 보여주는 영화 제목"),
                                fieldWithPath("data.shortDescription").type(JsonFieldType.STRING)
                                        .description("관리자가 확정한 짧은 소개"),
                                fieldWithPath("data.youtubeDescription").type(JsonFieldType.STRING).optional()
                                        .description("YouTube 원본 영상의 설명. 없으면 null"),
                                fieldWithPath("data.thumbnailUrl").type(JsonFieldType.STRING).description("썸네일 URL"),
                                fieldWithPath("data.youtubeVideoId").type(JsonFieldType.STRING)
                                        .description("YouTube 플레이어로 재생할 영상 ID"),
                                fieldWithPath("data.youtubeChannelTitle").type(JsonFieldType.STRING)
                                        .description("YouTube 채널 이름"),
                                fieldWithPath("data.runningTimeSeconds").type(JsonFieldType.NUMBER)
                                        .description("러닝타임(초)"),
                                fieldWithPath("data.youtubePublishedAt").type(JsonFieldType.STRING).optional()
                                        .description("YouTube 게시 시각. 없으면 null"),
                                fieldWithPath("data.approvedAt").type(JsonFieldType.STRING)
                                        .description("AI FILM 등록(승인) 시각"),
                                fieldWithPath("data.genres[].genreId").type(JsonFieldType.NUMBER).description("장르 ID"),
                                fieldWithPath("data.genres[].name").type(JsonFieldType.STRING).description("장르 이름"),
                                fieldWithPath("data.statistics.averageRating").type(JsonFieldType.NUMBER)
                                        .description("평균 평점 (0.00 ~ 5.00)"),
                                fieldWithPath("data.statistics.reviewCount").type(JsonFieldType.NUMBER)
                                        .description("리뷰 수"),
                                fieldWithPath("data.statistics.totalViewCount").type(JsonFieldType.NUMBER)
                                        .description("AI FILM 누적 조회수"),
                                fieldWithPath("data.badges[].type").type(JsonFieldType.STRING)
                                        .description("배지 종류. 지금은 NEW(승인 후 7일 이내)만 있습니다."),
                                fieldWithPath("data.badges[].label").type(JsonFieldType.STRING)
                                        .description("화면에 보여줄 배지 문구"),
                                fieldWithPath("data.badges[].rankPosition").type(JsonFieldType.NUMBER).optional()
                                        .description("순위 배지의 순위. 순위 배지가 아니면 null"))
                        .build())));
    }

    @Test
    @DisplayName("없는 영화를 조회하면 404와 MOVIE_404_1을 반환한다")
    void getMissingMovie() throws Exception {
        given(movieService.getMovieDetail(999L)).willThrow(new BusinessException(MovieErrorCode.MOVIE_NOT_FOUND));

        mockMvc.perform(get(MOVIE, 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MOVIE_404_1"))
                .andDo(document("movie-get-not-found", resource(parameters()
                        .responseFields(failureFields())
                        .build())));
    }

    @Test
    @DisplayName("볼 수 없는 영화를 조회하면 404와 MOVIE_404_2를 반환한다")
    void getUnavailableMovie() throws Exception {
        given(movieService.getMovieDetail(102L)).willThrow(new BusinessException(MovieErrorCode.MOVIE_UNAVAILABLE));

        mockMvc.perform(get(MOVIE, 102L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MOVIE_404_2"))
                .andDo(document("movie-get-unavailable", resource(parameters()
                        .responseFields(failureFields())
                        .build())));
    }

    @Test
    @DisplayName("영화 ID가 1보다 작으면 400을 반환하고 영화를 조회하지 않는다")
    void getMovieWithInvalidId() throws Exception {
        mockMvc.perform(get(MOVIE, 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_1"))
                .andExpect(jsonPath("$.errors[0].field").value("movieId"))
                .andDo(document("movie-get-invalid-id", resource(parameters()
                        .responseFields(failureFieldsWithErrors())
                        .build())));

        then(movieService).shouldHaveNoInteractions();
    }

    private static ResourceSnippetParametersBuilder parameters() {
        return ResourceSnippetParameters.builder()
                .tag(TAG)
                .summary(SUMMARY)
                .description(DESCRIPTION)
                // 타입을 지정하지 않으면 경로 변수가 스펙에 string으로 나간다
                .pathParameters(parameterWithName("movieId").type(SimpleType.INTEGER).description("영화 ID (1 이상)"));
    }

    private static List<FieldDescriptor> failureFields() {
        return List.of(
                fieldWithPath("status").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                fieldWithPath("code").type(JsonFieldType.STRING).description("에러 코드"),
                fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지"),
                fieldWithPath("data").type(JsonFieldType.NULL).description("실패 응답은 항상 null"));
    }

    private static List<FieldDescriptor> failureFieldsWithErrors() {
        List<FieldDescriptor> fields = new ArrayList<>(failureFields());
        fields.add(fieldWithPath("errors[].field").type(JsonFieldType.STRING).description("잘못된 입력 이름"));
        fields.add(fieldWithPath("errors[].reason").type(JsonFieldType.STRING).description("잘못된 이유"));
        return fields;
    }

    private static MovieDetailResponse sampleDetail() {
        return new MovieDetailResponse(101L, "네온 메모리", "기억은 사라져도, 사랑은 남는다.",
                "기억과 사랑을 주제로 제작한 AI 단편영화입니다.", "https://example.com/neon-memory.jpg", "abc123",
                "AI Film Studio", 766, LocalDateTime.of(2025, 1, 10, 13, 20),
                // DB에서 읽은 값처럼 마이크로초가 있어도 응답은 초까지만 나가는지 확인한다
                LocalDateTime.of(2026, 10, 20, 12, 0, 0, 123_456_000),
                List.of(new GenreResponse(1L, "SF"), new GenreResponse(5L, "로맨스")),
                new MovieStatisticsResponse(new BigDecimal("4.70"), 21, 0),
                List.of(MovieBadgeResponse.from(MovieBadgeType.NEW)));
    }

}
