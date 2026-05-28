package com.part3_team4.deokhoogam.global.exception;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

public record ErrorResponse(
    Instant timestamp,
    String code,
    String message,
    Map<String, Object> details,
    String exceptionType,
    int status
) {

  // 정적 팩토리 메서드: 커스텀 비즈니스 예외
  public static ErrorResponse from(BusinessException exception) {
    return new ErrorResponse(
        Instant.now(),
        exception.getErrorCode().getCode(),
        exception.getErrorCode().getMessage(),
        exception.getDetails(),
        exception.getClass().getSimpleName(),
        exception.getErrorCode().getStatus().value()
    );
  }

  // 정적 팩토리 메서드: 외부 예외 - 유효성 검사 등
  public static ErrorResponse of(ErrorCode errorCode, Exception exception,
      Map<String, Object> details) {
    return new ErrorResponse(
        Instant.now(),
        errorCode.getCode(),
        errorCode.getMessage(),
        details != null ? Map.copyOf(details) : Collections.emptyMap(), // null 방지
        exception.getClass().getSimpleName(),
        errorCode.getStatus().value()
    );
  }

  // 정적 팩토리 메서드: 외부 예외 - detail 불필요
  public static ErrorResponse of(ErrorCode errorCode, Exception exception) {
    return new ErrorResponse(
        Instant.now(),
        errorCode.getCode(),
        errorCode.getMessage(),
        Collections.emptyMap(),
        exception.getClass().getSimpleName(),
        errorCode.getStatus().value()
    );
  }
}