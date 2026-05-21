package com.part3_team4.deokhoogam.global.exception;


import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
    log.warn("Business Exception: {} - {}", e.getErrorCode(), e.getErrorCode().getMessage());

    ErrorCode errorCode = e.getErrorCode();

    ErrorResponse response = ErrorResponse.builder()
        .timestamp(e.getTimestamp())
        .code(errorCode.name())
        .message(errorCode.getMessage())
        .details(e.getDetails())
        .exceptionType(e.getClass().getSimpleName())
        .status(errorCode.getStatus().value())
        .build();

    return ResponseEntity.status(response.status()).body(response);

  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {

    Map<String, String> details = new HashMap<>();
    e.getBindingResult().getFieldErrors().forEach(error ->
        details.put(error.getField(), error.getDefaultMessage())
    );

    log.warn("Validation Exception: {}", details);

    ErrorResponse response = ErrorResponse.builder()
        .timestamp(Instant.now())
        .code("VALIDATION_ERROR")
        .message(e.getMessage())
        .details(details)
        .exceptionType(e.getClass().getSimpleName())
        .status(HttpStatus.BAD_REQUEST.value())
        .build();

    return ResponseEntity.badRequest().body(response);
  }

  @ExceptionHandler(value = {Exception.class})
  public ResponseEntity<ErrorResponse> handleException(Exception e) {

    ErrorResponse errorResponse = ErrorResponse.builder()
        .timestamp(Instant.now())
        .code("INTERNAL_SERVER_ERROR")
        .message(e.getMessage())
        .details(Map.of())
        .exceptionType(e.getClass().getSimpleName())
        .status(500)
        .build();

    return ResponseEntity.status(errorResponse.status()).body(errorResponse);

  }

}
