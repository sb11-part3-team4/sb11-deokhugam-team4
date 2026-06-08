package com.part3_team4.deokhoogam.global.exception;



public class Base64Exception extends BusinessException {

  public Base64Exception(ErrorCode errorCode) {
    super(errorCode);
  }

  public static Base64Exception EncodingError() {
    return new Base64Exception(ErrorCode.BASE64_ENCODING_ERROR);
  }
  public static Base64Exception DecodingError() {
    return new Base64Exception(ErrorCode.BASE64_DECODING_ERROR);
  }

}
