package com.aifilm.infra.mysql.home;

import com.aifilm.domain.home.HomeBanner;
import com.aifilm.domain.home.HomeBannerRepository;
import com.aifilm.domain.home.VisibleHomeBanner;
import com.aifilm.domain.movie.AvailabilityStatus;
import com.aifilm.domain.movie.ScreeningStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

// domain의 HomeBannerRepository를 Spring Data JPA로 구현한다.
@Repository
@RequiredArgsConstructor
public class HomeBannerRepositoryImpl implements HomeBannerRepository {

    private final JpaHomeBannerRepository jpaHomeBannerRepository;

    @Override
    public HomeBanner save(HomeBanner banner) {
        return jpaHomeBannerRepository.save(banner);
    }

    @Override
    public List<VisibleHomeBanner> findVisible(LocalDateTime now) {
        return jpaHomeBannerRepository.findVisibleBanners(
                now, ScreeningStatus.ACTIVE, AvailabilityStatus.AVAILABLE);
    }

}
