package com.aifilm.api.exception;

import com.aifilm.api.response.ApiResponse;
import com.aifilm.api.response.FieldErrorResponse;
import com.aifilm.common.error.BusinessException;
import com.aifilm.common.error.CommonErrorCode;
import com.aifilm.common.error.ErrorReason;
import com.aifilm.common.error.HttpStatusCodes;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

// 모든 예외를 ApiResponse 실패 응답으로 바꾼다. 응답 본문의 status와 HTTP 상태는 같은 ErrorReason에서 꺼내 일치시킨다.
// 스프링 MVC 예외는 부모 클래스가 받아 handleExceptionInternal로 모아 주므로 하나씩 나열하지 않는다.
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String TYPE_MISMATCH_REASON = "형식이 올바르지 않습니다.";
    private static final String MISSING_PARAMETER_REASON = "필수 값입니다.";

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorReason errorReason = e.getErrorReason();
        logException(e, errorReason);
        return ResponseEntity.status(errorReason.getStatus()).body(ApiResponse.fail(errorReason));
    }

    // 예상하지 못한 예외의 메시지에는 내부 구현이 드러날 수 있으므로 응답에는 고정 문구만 내린다.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception e) {
        ErrorReason errorReason = CommonErrorCode.INTERNAL_SERVER_ERROR;
        logException(e, errorReason);
        return ResponseEntity.status(errorReason.getStatus()).body(ApiResponse.fail(errorReason));
    }

    // @RequestBody @Valid 검증 실패. 클래스 단위 제약 오류는 필드 대신 객체 이름으로 담는다.
    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldErrorResponse> errors = ex.getBindingResult().getAllErrors().stream()
                .map(error -> new FieldErrorResponse(
                        error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName(),
                        error.getDefaultMessage()))
                .toList();
        return respond(ex, CommonErrorCode.INVALID_INPUT_VALUE, errors, headers, request);
    }

    // @PathVariable, @RequestParam에 붙인 제약 조건 위반
    @Override
    protected @Nullable ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        // 반환값 검증 실패는 클라이언트 입력이 아니라 서버 코드 문제이므로 500으로 응답하고 필드 오류를 담지 않는다.
        if (ex.isForReturnValue()) {
            return respond(ex, CommonErrorCode.INTERNAL_SERVER_ERROR, null, headers, request);
        }
        List<FieldErrorResponse> errors = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new FieldErrorResponse(
                                result.getMethodParameter().getParameterName(), error.getDefaultMessage())))
                .toList();
        return respond(ex, CommonErrorCode.INVALID_INPUT_VALUE, errors, headers, request);
    }

    // 입력값을 그대로 돌려주면 응답에 사용자 입력이 반사되므로 필드명과 고정 사유만 담는다.
    // 필드명을 알 수 없는 경우(빈 바인딩 등)에는 errors 없이 응답한다.
    @Override
    protected @Nullable ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldErrorResponse> errors = ex.getPropertyName() == null
                ? null
                : List.of(new FieldErrorResponse(ex.getPropertyName(), TYPE_MISMATCH_REASON));
        return respond(ex, CommonErrorCode.INVALID_INPUT_VALUE, errors, headers, request);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatusCode status,
            WebRequest request) {
        List<FieldErrorResponse> errors = List.of(
                new FieldErrorResponse(ex.getParameterName(), MISSING_PARAMETER_REASON));
        return respond(ex, CommonErrorCode.INVALID_INPUT_VALUE, errors, headers, request);
    }

    // JSON 문법 오류 등으로 본문 자체를 읽지 못해 특정 필드를 가리킬 수 없는 경우
    @Override
    protected @Nullable ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return respond(ex, CommonErrorCode.INVALID_REQUEST_BODY, null, headers, request);
    }

    // 개별 오버라이드하지 않은 스프링 MVC 예외는 모두 여기로 온다. 스프링이 만든 ProblemDetail 본문은 버리고 상태로만 매핑한다.
    @Override
    protected @Nullable ResponseEntity<Object> handleExceptionInternal(
            Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        return respond(ex, resolveByStatus(statusCode.value()), null, headers, request);
    }

    // 부모의 handleExceptionInternal을 거쳐 응답이 이미 커밋된 경우의 처리와 5xx 관측 기록을 그대로 쓴다.
    // 405의 Allow 헤더처럼 스프링이 채운 헤더도 유지된다.
    protected @Nullable ResponseEntity<Object> respond(Exception ex, ErrorReason errorReason,
            @Nullable List<FieldErrorResponse> errors, HttpHeaders headers, WebRequest request) {
        logException(ex, errorReason);
        return super.handleExceptionInternal(ex, ApiResponse.fail(errorReason, errors), headers,
                HttpStatusCode.valueOf(errorReason.getStatus()), request);
    }

    // 매핑되지 않은 상태는 4xx면 400, 5xx면 500으로 응답해 본문과 HTTP 상태를 항상 같은 값으로 맞춘다.
    private static ErrorReason resolveByStatus(int status) {
        return switch (status) {
            case HttpStatusCodes.NOT_FOUND -> CommonErrorCode.RESOURCE_NOT_FOUND;
            case HttpStatusCodes.METHOD_NOT_ALLOWED -> CommonErrorCode.METHOD_NOT_ALLOWED;
            case HttpStatusCodes.NOT_ACCEPTABLE -> CommonErrorCode.NOT_ACCEPTABLE;
            case HttpStatusCodes.PAYLOAD_TOO_LARGE -> CommonErrorCode.PAYLOAD_TOO_LARGE;
            case HttpStatusCodes.UNSUPPORTED_MEDIA_TYPE -> CommonErrorCode.UNSUPPORTED_MEDIA_TYPE;
            case HttpStatusCodes.SERVICE_UNAVAILABLE -> CommonErrorCode.SERVICE_UNAVAILABLE;
            default -> status < HttpStatusCodes.INTERNAL_SERVER_ERROR
                    ? CommonErrorCode.BAD_REQUEST
                    : CommonErrorCode.INTERNAL_SERVER_ERROR;
        };
    }

    // 4xx는 클라이언트 잘못이라 한 줄만, 5xx는 원인 추적을 위해 스택트레이스까지 남긴다.
    private void logException(Exception e, ErrorReason errorReason) {
        if (errorReason.getStatus() >= HttpStatusCodes.INTERNAL_SERVER_ERROR) {
            log.error("[{}] {}", errorReason.getCode(), e.getMessage(), e);
            return;
        }
        log.warn("[{}] {}: {}", errorReason.getCode(), e.getClass().getSimpleName(), e.getMessage());
    }

}
