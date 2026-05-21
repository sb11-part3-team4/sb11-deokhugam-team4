package com.part3_team4.deokhoogam.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode{

  //1. 유저 예외



  //2. 도서 예외


  //3. 댓글 예외


  //4. 리뷰 예외


  //5. 알림 예외


  //6. 공용 예외




  DUMMY_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "더미 예외입니다.");

  private final HttpStatus status;
  private final String message;

}
