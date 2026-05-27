package com.part3_team4.deokhoogam.global.exception;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  // 커스텀 비즈니스 예외
  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
    log.warn("Business Exception: {} - {}", e.getErrorCode().getCode(),
        e.getErrorCode().getMessage());
    ErrorResponse response = ErrorResponse.from(e);

    return ResponseEntity
        .status(response.status())
        .body(response);
  }

  // @Valid 유효성 검사 실패 예외
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(
      MethodArgumentNotValidException e) {
    Map<String, Object> details = new HashMap<>();
    e.getBindingResult().getFieldErrors().forEach(error ->
        details.put(error.getField(), error.getDefaultMessage())
    );
    log.warn("Validation Exception: {}", details);

    ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
    ErrorResponse response = ErrorResponse.of(errorCode, e, details);

    return ResponseEntity
        .status(errorCode.getStatus())
        .body(response);
  }

  // 나머지 모든 예외
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception e) {
    log.error("Unhandled Global Exception", e);

    ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
    ErrorResponse response = ErrorResponse.of(errorCode, e);

    return ResponseEntity
        .status(errorCode.getStatus())
        .body(response);
  }
}