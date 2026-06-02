package com.part3_team4.deokhoogam.global.exception.storage;

import com.part3_team4.deokhoogam.global.exception.ErrorCode;
import com.part3_team4.deokhoogam.global.exception.ErrorKey;

public class StorageOperationException extends StorageException {

  private StorageOperationException(Throwable cause) {
    // 💡 ErrorCode.STORAGE_UPLOAD_FAILED 가 추가되어야 합니다.
    super(ErrorCode.STORAGE_UPLOAD_FAILED, cause);
  }

  // 어떤 S3 Key(파일명)에서 장애가 발생했는지 상세 기록
  public static StorageOperationException uploadFailed(String key, Throwable cause) {
    StorageOperationException exception = new StorageOperationException(cause);

    // ErrorKey.TARGET 또는 ErrorKey.FIELD 등 팀 컨벤션에 맞는 Key 사용
    exception.addDetail(ErrorKey.FIELD, "s3_key");
    exception.addDetail(ErrorKey.VALUE, key);
    exception.addDetail(ErrorKey.REASON, "S3 인프라 업로드 중 예외 발생");

    return exception;
  }
}