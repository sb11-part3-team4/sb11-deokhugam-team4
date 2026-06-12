package com.part3_team4.deokhoogam.global.exception;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  // 커스텀 비즈니스 예외
  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
    ErrorCode errorCode = e.getErrorCode();

    // 5xx는 서버/외부 장애 → ERROR(스택트레이스), 4xx는 클라이언트 문제 → WARN
    if (errorCode.getStatus().is5xxServerError()) {
      log.error("Business Exception (server): {} - {}",
          errorCode.getCode(), errorCode.getMessage(), e);
    } else {
      log.warn("Business Exception (client): {} - {}",
          errorCode.getCode(), errorCode.getMessage());
    }
    ErrorResponse response = ErrorResponse.from(e);
    return ResponseEntity
        .status(response.status())
        .body(response);
  }

  // @Valid 유효성 검사 실패 예외
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(
      MethodArgumentNotValidException e) {
    Map<String, List<String>> fieldErrors = e.getBindingResult()
        .getFieldErrors().stream()
        .collect(Collectors.groupingBy(
            FieldError::getField,
            Collectors.mapping(FieldError::getDefaultMessage, Collectors.toList())
        ));

    Map<String, Object> details = new HashMap<>(fieldErrors);
    log.warn("Validation Exception: {}", details);

    ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
    ErrorResponse response = ErrorResponse.of(errorCode, e, details);

    return ResponseEntity
        .status(errorCode.getStatus())
        .body(response);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
    log.warn("Type Mismatch: {}", e.getMessage());
    ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
    ErrorResponse response = ErrorResponse.of(errorCode, e);
    return ResponseEntity.status(errorCode.getStatus()).body(response);
  }

  @ExceptionHandler(MissingRequestHeaderException.class)
  public ResponseEntity<ErrorResponse>
  handleMissingHeader(MissingRequestHeaderException e) {
    log.warn("Missing Header: {}", e.getHeaderName());
    ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
    ErrorResponse response = ErrorResponse.of(errorCode, e);
    return ResponseEntity.status(errorCode.getStatus()).body(response);
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