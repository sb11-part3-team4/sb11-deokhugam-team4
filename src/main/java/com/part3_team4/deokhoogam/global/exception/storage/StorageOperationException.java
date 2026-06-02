package com.part3_team4.deokhoogam.global.exception.storage;

public class StorageOperationException extends RuntimeException {

  public static StorageOperationException uploadFailed(String filename) {
    return new StorageOperationException();
  }

 /* public StorageOperationException(String message, Throwable cause) {
    super(message, cause);
  }

  public static StorageOperationException uploadFailed(String key, Throwable cause
  ) {
    return new StorageOperationException("S3 업로드 실패: " + key, cause);
  }*/
}
