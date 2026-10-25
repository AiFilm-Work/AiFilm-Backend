package com.aifilm.api.movie;

import jakarta.validation.constraints.Positive;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.aifilm.api.movie.dto.MovieDetailResponse;
import com.aifilm.api.response.ApiResponse;

@RestController
@RequiredArgsConstructor
public class MovieController {

	private final MovieService movieService;

	@GetMapping("/api/v1/movies/{movieId}")
	public ApiResponse<MovieDetailResponse> getMovieDetail(@PathVariable @Positive Long movieId) {
		return ApiResponse.success(movieService.getMovieDetail(movieId));
	}

}
