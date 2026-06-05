package com.part3_team4.deokhoogam.global.exception.storage;

import com.part3_team4.deokhoogam.global.exception.ErrorCode;
import com.part3_team4.deokhoogam.global.exception.ErrorKey;

public class InvalidFileException extends StorageException {

  private InvalidFileException() {
    super(ErrorCode.INVALID_FILE_INPUT);
  }

  public static InvalidFileException withField(ErrorKey field, String reason) {
    InvalidFileException exception = new InvalidFileException();
    exception.addDetail(ErrorKey.FIELD, field.getValue());
    exception.addDetail(ErrorKey.REASON, reason);
    return exception;
  }
  
  public static InvalidFileException withFieldAndValue(
      ErrorKey field, Object value, String reason) {
    InvalidFileException exception = new InvalidFileException();
    exception.addDetail(ErrorKey.FIELD, field.getValue());
    exception.addDetail(ErrorKey.VALUE, value);
    exception.addDetail(ErrorKey.REASON, reason);
    return exception;
  }
}