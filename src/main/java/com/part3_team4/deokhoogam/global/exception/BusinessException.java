package com.part3_team4.deokhoogam.global.exception;

import java.time.Instant;
import java.util.Map;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException{

  private final Instant timestamp;
  private final ErrorCode errorCode;
  private final Map<String,Object> details;

  public BusinessException(ErrorCode errorCode,Map<String,Object> details){
    super(errorCode.getMessage());
    this.timestamp = Instant.now();
    this.errorCode = errorCode;
    this.details = details == null ? Map.of() : details;
  }



}
