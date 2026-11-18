package com.aifilm.api.exception;

import com.aifilm.common.error.ErrorReason;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
enum ExceptionTestErrorCode implements ErrorReason {

    CONFLICT(409, "TEST_409_1", "테스트 리소스가 이미 존재합니다.");

    private final int status;
    private final String code;
    private final String message;

}
