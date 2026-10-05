package com.aifilm.common.error;

// common은 스프링에 의존하지 않으므로 HTTP 상태를 HttpStatus 대신 int로 표현한다.
public interface ErrorReason {

	int getStatus();

	String getCode();

	String getMessage();

}
