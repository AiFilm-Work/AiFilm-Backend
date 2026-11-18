package com.aifilm.api.exception;

import com.aifilm.common.error.BusinessException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 전역 예외 핸들러가 예외 종류별로 어떤 응답을 내리는지 확인하기 위한 테스트 전용 컨트롤러
@RestController
class ExceptionTestController {

    @GetMapping("/test/exceptions/business")
    public void business() {
        throw new BusinessException(ExceptionTestErrorCode.CONFLICT);
    }

    @GetMapping("/test/exceptions/unexpected")
    public void unexpected() {
        throw new IllegalStateException("내부 구현 정보가 담긴 메시지");
    }

    @PostMapping(value = "/test/exceptions/json", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void json(@RequestBody Map<String, Object> body) {
    }

    @PostMapping("/test/exceptions/body")
    public void body(@Valid @RequestBody ExceptionTestRequest request) {
    }

    @GetMapping("/test/exceptions/param")
    public void param(@RequestParam int count) {
    }

    @GetMapping("/test/exceptions/path/{id}")
    public void path(@PathVariable @Positive long id) {
    }

    // 반환값 제약을 어기는 응답을 돌려줘 반환값 검증 실패를 일으킨다.
    @GetMapping("/test/exceptions/return-value")
    public @NotBlank String returnValue() {
        return "";
    }

    record ExceptionTestRequest(@NotBlank String name) {
    }

}
