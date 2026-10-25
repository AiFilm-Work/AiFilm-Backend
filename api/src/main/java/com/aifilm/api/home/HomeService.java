package com.aifilm.api.home;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aifilm.api.genre.dto.GenreResponse;
import com.aifilm.api.home.dto.HomeBannerResponse;
import com.aifilm.api.home.dto.HomeMovieResponse;
import com.aifilm.api.home.dto.HomeResponse;
import com.aifilm.api.home.dto.HomeSectionResponse;
import com.aifilm.api.home.dto.HomeSectionType;
import com.aifilm.domain.genre.GenreRepository;
import com.aifilm.domain.genre.MovieGenre;
import com.aifilm.domain.home.HomeBannerRepository;
import com.aifilm.domain.home.VisibleHomeBanner;
import com.aifilm.domain.movie.MovieCard;
import com.aifilm.domain.movie.MovieRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

	// 섹션마다 보여줄 영화 수. 화면의 "평점 TOP 10"과 맞춘다
	static final int SECTION_SIZE = 10;

	private final HomeBannerRepository homeBannerRepository;
	private final GenreRepository genreRepository;
	private final MovieRepository movieRepository;
	private final Clock clock;

	// 배너, 배너 장르, 장르 메뉴, 최신 등록작, 평점 TOP을 쿼리 한 번씩으로 조회한다. 영화가 늘어도 쿼리 수는 같다.
	// 한 트랜잭션으로 묶어 다섯 쿼리가 같은 시점의 데이터를 읽게 한다.
	public HomeResponse getHome() {
		List<VisibleHomeBanner> banners = homeBannerRepository.findVisible(LocalDateTime.now(clock));
		return new HomeResponse(
			toBannerResponses(banners),
			genreRepository.findAllOrderById().stream().map(GenreResponse::from).toList(),
			List.of(
				toSection(HomeSectionType.LATEST, movieRepository.findLatestScreenable(SECTION_SIZE + 1)),
				toSection(HomeSectionType.RATING_TOP, movieRepository.findTopRatedScreenable(SECTION_SIZE + 1))));
	}

	// 배너 영화들의 장르는 영화마다 조회하지 않고 한 번에 가져와 영화 ID로 나눠 붙인다
	private List<HomeBannerResponse> toBannerResponses(List<VisibleHomeBanner> banners) {
		List<Long> movieIds = banners.stream().map(VisibleHomeBanner::movieId).toList();
		Map<Long, List<GenreResponse>> genresByMovieId = genreRepository.findByMovieIds(movieIds).stream()
			.collect(Collectors.groupingBy(MovieGenre::movieId,
				Collectors.mapping(GenreResponse::from, Collectors.toList())));
		return banners.stream()
			.map(banner -> HomeBannerResponse.of(banner, genresByMovieId.getOrDefault(banner.movieId(), List.of())))
			.toList();
	}

	// 한 편 더 조회해서 남는 영화가 있으면 더보기가 있다고 알려준다. 개수 쿼리를 따로 두지 않기 위해서다.
	private HomeSectionResponse toSection(HomeSectionType type, List<MovieCard> fetched) {
		boolean hasMore = fetched.size() > SECTION_SIZE;
		List<MovieCard> cards = hasMore ? fetched.subList(0, SECTION_SIZE) : fetched;
		List<HomeMovieResponse> movies = IntStream.range(0, cards.size())
			.mapToObj(index -> HomeMovieResponse.of(cards.get(index), type.isRanked() ? index + 1 : null))
			.toList();
		return new HomeSectionResponse(type, type.getTitle(), null, hasMore, movies);
	}

}
