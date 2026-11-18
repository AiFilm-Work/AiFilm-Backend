package com.aifilm.api.movie.dto;

import com.aifilm.domain.movie.MovieDetail;
import java.math.BigDecimal;

// 영화 통계. 조회수는 AI FILM 안에서 센 값이다. (상세 조회로는 올리지 않는다)
public record MovieStatisticsResponse(BigDecimal averageRating, long reviewCount, long totalViewCount) {

    public static MovieStatisticsResponse from(MovieDetail detail) {
        return new MovieStatisticsResponse(detail.averageRating(), detail.reviewCount(), detail.totalViewCount());
    }

}
