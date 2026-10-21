package com.aifilm.infra.mysql.home;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import com.aifilm.domain.home.HomeBanner;
import com.aifilm.domain.home.HomeBannerRepository;

// domain의 HomeBannerRepository를 Spring Data JPA로 구현한다.
@Repository
@RequiredArgsConstructor
public class HomeBannerRepositoryImpl implements HomeBannerRepository {

	private final JpaHomeBannerRepository jpaHomeBannerRepository;

	@Override
	public HomeBanner save(HomeBanner banner) {
		return jpaHomeBannerRepository.save(banner);
	}

}
