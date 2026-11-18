package com.aifilm.common.error;

// common은 스프링에 의존하지 않으므로 에러 코드 enum에서 쓸 HTTP 상태를 int 상수로 둔다.
// switch 문의 case로 쓸 수 있도록 컴파일 타임 상수로 선언한다.
public final class HttpStatusCodes {

    public static final int BAD_REQUEST = 400;
    public static final int NOT_FOUND = 404;
    public static final int METHOD_NOT_ALLOWED = 405;
    public static final int NOT_ACCEPTABLE = 406;
    public static final int PAYLOAD_TOO_LARGE = 413;
    public static final int UNSUPPORTED_MEDIA_TYPE = 415;
    public static final int INTERNAL_SERVER_ERROR = 500;
    public static final int SERVICE_UNAVAILABLE = 503;

    private HttpStatusCodes() {
    }

}
