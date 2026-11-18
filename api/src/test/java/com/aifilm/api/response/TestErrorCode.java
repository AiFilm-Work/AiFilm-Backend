package com.aifilm.api.response;

import com.aifilm.common.error.ErrorReason;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
enum TestErrorCode implements ErrorReason {

    INVALID(400, "TEST_400_1", "테스트 입력값이 올바르지 않습니다."),
    NOT_FOUND(404, "TEST_404_1", "테스트 리소스를 찾을 수 없습니다.");

    private final int status;
    private final String code;
    private final String message;

}
