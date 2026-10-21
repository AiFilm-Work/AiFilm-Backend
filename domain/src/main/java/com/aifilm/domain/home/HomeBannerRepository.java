package com.aifilm.domain.home;

// 홈 배너 저장소. 구현은 infra 모듈이 한다.
public interface HomeBannerRepository {

	HomeBanner save(HomeBanner banner);

}
