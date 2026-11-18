package com.aifilm.api.movie.dto;

import com.aifilm.api.genre.dto.GenreResponse;
import com.aifilm.api.response.ApiDateTimeFormat;
import com.aifilm.domain.movie.MovieDetail;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;

// 영화 상세 화면. 프론트는 youtubeVideoId로 YouTube 플레이어를 띄운다.
// 로그인 회원의 상태(viewer)는 로그인 기능이 생기면 필드로 추가한다.
public record MovieDetailResponse(Long movieId, String title, String shortDescription, String youtubeDescription,
        String thumbnailUrl, String youtubeVideoId, String youtubeChannelTitle, int runningTimeSeconds,
        @JsonFormat(pattern = ApiDateTimeFormat.PATTERN) LocalDateTime youtubePublishedAt,
        @JsonFormat(pattern = ApiDateTimeFormat.PATTERN) LocalDateTime approvedAt,
        List<GenreResponse> genres, MovieStatisticsResponse statistics, List<MovieBadgeResponse> badges) {

    public static MovieDetailResponse of(MovieDetail detail, List<GenreResponse> genres,
            List<MovieBadgeResponse> badges) {
        return new MovieDetailResponse(detail.movieId(), detail.title(), detail.shortDescription(),
                detail.youtubeDescription(), detail.thumbnailUrl(), detail.youtubeVideoId(),
                detail.youtubeChannelTitle(),
                detail.runningTimeSeconds(), detail.youtubePublishedAt(), detail.approvedAt(), genres,
                MovieStatisticsResponse.from(detail), badges);
    }

}
