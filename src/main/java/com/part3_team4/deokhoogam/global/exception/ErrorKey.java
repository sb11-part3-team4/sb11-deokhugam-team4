package com.part3_team4.deokhoogam.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorKey {

  /// ==== Common ====

  /// ==== 도메인  ====
  // 도서
  BOOK_ID("bookId"),

  // 사용자
  USER_ID("userId"),

  // 리뷰
  REVIEW_ID("reviewId"),

  // 댓글
  COMMENT_ID("commentId"),

  // 알림
  NOTIFICATION_ID("notificationId");
  
  private final String value;
}