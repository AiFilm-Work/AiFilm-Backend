package com.aifilm.api.home.dto;

import java.util.List;

// 홈 영화 섹션 하나. genreId는 장르 기반 섹션에서만 채우고 지금 섹션들은 null이다.
// hasMore는 보여준 영화 뒤에 노출 가능한 영화가 더 있는지 알려준다.
public record HomeSectionResponse(HomeSectionType sectionType, String title, Long genreId, boolean hasMore,
        List<HomeMovieResponse> movies) {
}
