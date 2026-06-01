package com.part3_team4.deokhoogam.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

  // 1. 사용자 예외
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER-001", "존재하지 않는 사용자입니다."),
  USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER-002", "이미 존재하는 사용자입니다."),

  // 2. 도서 예외
  BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "BOOK-001", "존재하지 않는 도서입니다."),
  INVALID_BOOK_INPUT(HttpStatus.BAD_REQUEST, "BOOK-002", "올바르지 않은 도서 정보입니다."),
  ISBN_ALREADY_EXISTS(HttpStatus.CONFLICT, "BOOK-003", "이미 존재하는 ISBN입니다."),

  // 3. 댓글 예외
  COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT-001", "존재하지 않는 댓글입니다."),
  COMMENT_NOT_OWNER(HttpStatus.FORBIDDEN, "COMMENT-002", "본인이 작성한 댓글만 수정/삭제할 수 있습니다."),
  COMMENT_CONTENT_REQUIRED(HttpStatus.BAD_REQUEST, "COMMENT-003", "댓글 내용은 필수입니다."),
  COMMENT_CONTENT_TOO_LONG(HttpStatus.BAD_REQUEST, "COMMENT-004", "댓글 내용은 최대 1000자까지 입력 가능합니다."),

  // 4. 리뷰 예외
  REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "REVIEW-001", "존재하지 않는 리뷰입니다."),
  REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, "REVIEW-002", "이미 해당 도서에 작성한 리뷰가 존재합니다."),

  // 5. 알림 예외
  NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION-001", "존재하지 않는 알림입니다."),
  NOTIFICATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "NOTIFICATION-002", "알림에 접근할 권한이 없습니다."),

  // 6. 공용 예외
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON-001", "서버 내부 오류가 발생했습니다."),
  INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON-002", "잘못된 입력값입니다."),
  BASE64_ENCODING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON-004", "커서 인코딩 중 오류가 발생했습니다."),
  BASE64_DECODING_ERROR(HttpStatus.BAD_REQUEST, "COMMON-004", "잘못된 커서로 인해 디코딩에 실패했습니다.");


  private final HttpStatus status;
  private final String code;
  private final String message;
}
