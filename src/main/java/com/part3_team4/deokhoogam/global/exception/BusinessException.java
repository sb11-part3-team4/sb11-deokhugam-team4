package com.part3_team4.deokhoogam.global.exception;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

public abstract class BusinessException extends RuntimeException {

  @Getter
  private final ErrorCode errorCode;

  private final Map<String, Object> details = new HashMap<>();

  protected BusinessException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }

  protected BusinessException(ErrorCode errorCode, Throwable cause) {
    super(errorCode.getMessage(), cause);
    this.errorCode = errorCode;
  }

  // 유틸 메서드: 상세 정보 추가
  protected void addDetail(ErrorKey key, Object value) {
    this.details.put(key.getValue(), value);
  }
  
  public Map<String, Object> getDetails() {
    return Collections.unmodifiableMap(details);
  }
}