package com.aifilm.common.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 특정 도메인에 속하지 않는 요청 오류와 서버 오류
@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorReason {

    INVALID_INPUT_VALUE(HttpStatusCodes.BAD_REQUEST, "COMMON_400_1", "입력값이 올바르지 않습니다."),
    INVALID_REQUEST_BODY(HttpStatusCodes.BAD_REQUEST, "COMMON_400_2", "요청 본문을 읽을 수 없습니다."),
    BAD_REQUEST(HttpStatusCodes.BAD_REQUEST, "COMMON_400_3", "잘못된 요청입니다."),
    RESOURCE_NOT_FOUND(HttpStatusCodes.NOT_FOUND, "COMMON_404_1", "요청한 경로를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatusCodes.METHOD_NOT_ALLOWED, "COMMON_405_1", "지원하지 않는 HTTP 메서드입니다."),
    NOT_ACCEPTABLE(HttpStatusCodes.NOT_ACCEPTABLE, "COMMON_406_1", "지원하지 않는 응답 형식입니다."),
    PAYLOAD_TOO_LARGE(HttpStatusCodes.PAYLOAD_TOO_LARGE, "COMMON_413_1", "요청 크기가 허용 범위를 넘었습니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatusCodes.UNSUPPORTED_MEDIA_TYPE, "COMMON_415_1", "지원하지 않는 Content-Type입니다."),
    INTERNAL_SERVER_ERROR(HttpStatusCodes.INTERNAL_SERVER_ERROR, "COMMON_500_1", "서버 내부 오류가 발생했습니다."),
    SERVICE_UNAVAILABLE(HttpStatusCodes.SERVICE_UNAVAILABLE, "COMMON_503_1", "일시적으로 요청을 처리할 수 없습니다.");

    private final int status;
    private final String code;
    private final String message;

}
