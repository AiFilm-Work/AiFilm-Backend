package com.aifilm.infra.mysql.home;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.aifilm.domain.home.HomeBanner;
import com.aifilm.domain.home.VisibleHomeBanner;
import com.aifilm.domain.movie.AvailabilityStatus;
import com.aifilm.domain.movie.ScreeningStatus;

// Spring Data JPA 리포지토리. 서비스 코드는 이 인터페이스 대신 domain의 HomeBannerRepository를 쓴다.
public interface JpaHomeBannerRepository extends JpaRepository<HomeBanner, Long> {

	// 배너는 영화를 ID로만 참조하므로 엔티티 조인으로 영화의 표시 정보를 함께 가져온다.
	// 영상 ID는 youtube_movie_source에 있다. 업로드 원본만 있는 영화도 배너에서 빠지지 않게 movie 기준으로 LEFT JOIN한다.
	// 노출 기간은 시작 시각을 포함하고 종료 시각은 포함하지 않는다.
	@Query("""
		select new com.aifilm.domain.home.VisibleHomeBanner(
		    b.id, m.id, m.title, m.shortDescription, m.thumbnailUrl, y.youtubeVideoId,
		    m.averageRating, m.reviewCount, b.startSeconds, b.previewDurationSeconds)
		from HomeBanner b
		    join Movie m on m.id = b.movieId
		    left join YoutubeMovieSource y on y.movieId = m.id
		where b.active = true
		  and (b.startsAt is null or b.startsAt <= :now)
		  and (b.endsAt is null or b.endsAt > :now)
		  and m.screeningStatus = :screeningStatus
		  and m.availabilityStatus = :availabilityStatus
		order by b.displayOrder asc, b.id asc
		""")
	List<VisibleHomeBanner> findVisibleBanners(@Param("now") LocalDateTime now,
		@Param("screeningStatus") ScreeningStatus screeningStatus,
		@Param("availabilityStatus") AvailabilityStatus availabilityStatus);

}
