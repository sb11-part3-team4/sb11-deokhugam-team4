package com.part3_team4.deokhoogam.global.exception.storage;

public class StorageOperationException extends RuntimeException {

  public static StorageOperationException uploadFailed(String filename) {
    return new StorageOperationException();
  }
}
