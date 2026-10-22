package com.aifilm.domain.movie;

import java.math.BigDecimal;

// 홈 섹션·목록에서 영화를 카드 하나로 보여줄 때 필요한 값만 담은 조회 모델.
// 엔티티를 불러오지 않고 필요한 컬럼만 조회해서 만든다.
public record MovieCard(Long movieId, String title, String thumbnailUrl, BigDecimal averageRating, long reviewCount) {
}
