package com.aifilm.api.home;

import com.aifilm.api.home.dto.HomeResponse;
import com.aifilm.api.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @GetMapping("/api/v1/home")
    public ApiResponse<HomeResponse> getHome() {
        return ApiResponse.success(homeService.getHome());
    }

}
