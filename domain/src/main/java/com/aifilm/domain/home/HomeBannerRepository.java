package com.aifilm.domain.home;

import java.time.LocalDateTime;
import java.util.List;

// 홈 배너 저장소. 구현은 infra 모듈이 한다.
public interface HomeBannerRepository {

    HomeBanner save(HomeBanner banner);

    // now 시점에 노출되는 배너를 노출 순서대로 조회한다.
    // 배너가 활성이고 노출 기간 안이어야 하며, 영화는 노출 중이고 재생 가능해야 한다.
    List<VisibleHomeBanner> findVisible(LocalDateTime now);

}
