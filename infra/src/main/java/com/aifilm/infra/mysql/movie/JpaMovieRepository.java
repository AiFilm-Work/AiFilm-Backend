package com.aifilm.infra.mysql.movie;

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.aifilm.domain.movie.Movie;
import com.aifilm.domain.movie.MovieCard;
import com.aifilm.domain.movie.ScreeningStatus;
import com.aifilm.domain.movie.YoutubeAvailabilityStatus;

// Spring Data JPA 리포지토리. 서비스 코드는 이 인터페이스 대신 domain의 MovieRepository를 쓴다.
public interface JpaMovieRepository extends JpaRepository<Movie, Long> {

	// 엔티티로 조회하면 카드마다 genreIds 지연 로딩이 따라올 수 있어서 카드에 필요한 컬럼만 생성자 표현식으로 조회한다
	@Query("""
		select new com.aifilm.domain.movie.MovieCard(m.id, m.title, m.thumbnailUrl, m.averageRating, m.reviewCount)
		from Movie m
		where m.screeningStatus = :screeningStatus
		  and m.youtubeAvailabilityStatus = :availabilityStatus
		order by m.approvedAt desc, m.id desc
		""")
	List<MovieCard> findLatestCards(@Param("screeningStatus") ScreeningStatus screeningStatus,
		@Param("availabilityStatus") YoutubeAvailabilityStatus availabilityStatus, Limit limit);

	// 평점이 같으면 리뷰가 많은 영화, 그것도 같으면 최근에 승인된 영화가 앞선다
	@Query("""
		select new com.aifilm.domain.movie.MovieCard(m.id, m.title, m.thumbnailUrl, m.averageRating, m.reviewCount)
		from Movie m
		where m.screeningStatus = :screeningStatus
		  and m.youtubeAvailabilityStatus = :availabilityStatus
		order by m.averageRating desc, m.reviewCount desc, m.approvedAt desc, m.id desc
		""")
	List<MovieCard> findTopRatedCards(@Param("screeningStatus") ScreeningStatus screeningStatus,
		@Param("availabilityStatus") YoutubeAvailabilityStatus availabilityStatus, Limit limit);

}
