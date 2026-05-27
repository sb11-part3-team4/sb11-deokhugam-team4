package com.part3_team4.deokhoogam.domain.comment.exception;

import com.part3_team4.deokhoogam.global.exception.ErrorCode;
import com.part3_team4.deokhoogam.global.exception.ErrorKey;
import java.util.UUID;

public class CommentNotFoundException extends CommentException {

  private CommentNotFoundException() {
    super(ErrorCode.COMMENT_NOT_FOUND);
  }

  public static CommentNotFoundException withId(UUID commentId) {
    CommentNotFoundException exception = new CommentNotFoundException();
    exception.addDetail(ErrorKey.COMMENT_ID, commentId);
    return exception;
  }
}