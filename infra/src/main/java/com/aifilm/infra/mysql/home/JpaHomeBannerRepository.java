package com.aifilm.infra.mysql.home;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.aifilm.domain.home.HomeBanner;
import com.aifilm.domain.home.VisibleHomeBanner;
import com.aifilm.domain.movie.ScreeningStatus;
import com.aifilm.domain.movie.YoutubeAvailabilityStatus;

// Spring Data JPA 리포지토리. 서비스 코드는 이 인터페이스 대신 domain의 HomeBannerRepository를 쓴다.
public interface JpaHomeBannerRepository extends JpaRepository<HomeBanner, Long> {

	// 배너는 영화를 ID로만 참조하므로 엔티티 조인으로 영화의 표시 정보를 함께 가져온다.
	// 노출 기간은 시작 시각을 포함하고 종료 시각은 포함하지 않는다.
	@Query("""
		select new com.aifilm.domain.home.VisibleHomeBanner(
		    b.id, m.id, m.title, m.shortDescription, m.thumbnailUrl, m.youtubeVideoId,
		    m.averageRating, m.reviewCount, b.startSeconds, b.previewDurationSeconds)
		from HomeBanner b
		    join Movie m on m.id = b.movieId
		where b.active = true
		  and (b.startsAt is null or b.startsAt <= :now)
		  and (b.endsAt is null or b.endsAt > :now)
		  and m.screeningStatus = :screeningStatus
		  and m.youtubeAvailabilityStatus = :availabilityStatus
		order by b.displayOrder asc, b.id asc
		""")
	List<VisibleHomeBanner> findVisibleBanners(@Param("now") LocalDateTime now,
		@Param("screeningStatus") ScreeningStatus screeningStatus,
		@Param("availabilityStatus") YoutubeAvailabilityStatus availabilityStatus);

}
