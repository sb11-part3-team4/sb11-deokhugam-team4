package com.part3_team4.deokhoogam.global.exception.storage;

import com.part3_team4.deokhoogam.global.exception.ErrorCode;
import com.part3_team4.deokhoogam.global.exception.ErrorKey;

public class StorageOperationException extends StorageException {

  private StorageOperationException(ErrorCode errorCode, Throwable cause) {
    super(errorCode, cause);
  }

  public static StorageOperationException uploadFailed(String key, Throwable cause) {
    StorageOperationException exception =
        new StorageOperationException(ErrorCode.STORAGE_UPLOAD_FAILED, cause);

    exception.addDetail(ErrorKey.FIELD, "s3_key");
    exception.addDetail(ErrorKey.VALUE, key);
    exception.addDetail(ErrorKey.REASON, "S3 인프라 업로드 중 예외 발생");

    return exception;
  }
}