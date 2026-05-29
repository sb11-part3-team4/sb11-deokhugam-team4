package com.part3_team4.deokhoogam.domain.comment.exception;

import com.part3_team4.deokhoogam.global.exception.ErrorCode;
import com.part3_team4.deokhoogam.global.exception.ErrorKey;
import java.util.UUID;

public class CommentNotOwnerException extends CommentException {

    private CommentNotOwnerException() {
        super(ErrorCode.COMMENT_NOT_OWNER);
    }

    public static CommentNotOwnerException forUser(UUID userId) {
        CommentNotOwnerException exception = new CommentNotOwnerException();
        exception.addDetail(ErrorKey.USER_ID, userId);
        return exception;
    }
}