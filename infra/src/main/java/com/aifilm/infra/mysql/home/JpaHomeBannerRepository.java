package com.aifilm.infra.mysql.home;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aifilm.domain.home.HomeBanner;

// Spring Data JPA 리포지토리. 서비스 코드는 이 인터페이스 대신 domain의 HomeBannerRepository를 쓴다.
public interface JpaHomeBannerRepository extends JpaRepository<HomeBanner, Long> {
}
