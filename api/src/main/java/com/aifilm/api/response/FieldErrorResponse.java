package com.aifilm.api.response;

// 검증에 실패한 필드와 사유. 사용자가 보낸 값은 민감 정보일 수 있어 담지 않는다.
public record FieldErrorResponse(String field, String reason) {
}
