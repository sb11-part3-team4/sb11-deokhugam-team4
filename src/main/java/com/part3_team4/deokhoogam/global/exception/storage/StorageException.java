package com.part3_team4.deokhoogam.global.exception.storage;

import com.part3_team4.deokhoogam.global.exception.BusinessException;
import com.part3_team4.deokhoogam.global.exception.ErrorCode;

public abstract class StorageException extends BusinessException {

  protected StorageException(ErrorCode errorCode) {
    super(errorCode);
  }

  protected StorageException(ErrorCode errorCode, Throwable cause) {
    super(errorCode, cause);
  }
}