package com.part3_team4.deokhoogam.global.exception.storage;

import com.part3_team4.deokhoogam.global.exception.ErrorCode;
import com.part3_team4.deokhoogam.global.exception.ErrorKey;

public class InvalidFileException extends StorageException {

  private InvalidFileException() {
    // 💡 ErrorCode.INVALID_FILE_INPUT 이 추가되어야 합니다.
    super(ErrorCode.INVALID_FILE_INPUT);
  }

  // 필드와 이유만 전달 (예: null, 빈 파일)
  public static InvalidFileException withField(ErrorKey field, String reason) {
    InvalidFileException exception = new InvalidFileException();
    exception.addDetail(ErrorKey.FIELD, field.getValue());
    exception.addDetail(ErrorKey.REASON, reason);
    return exception;
  }

  // 필드, 값, 이유를 함께 전달 (예: 확장자 불일치)
  public static InvalidFileException withFieldAndValue(
      ErrorKey field, Object value, String reason) {
    InvalidFileException exception = new InvalidFileException();
    exception.addDetail(ErrorKey.FIELD, field.getValue());
    exception.addDetail(ErrorKey.VALUE, value);
    exception.addDetail(ErrorKey.REASON, reason);
    return exception;
  }
}