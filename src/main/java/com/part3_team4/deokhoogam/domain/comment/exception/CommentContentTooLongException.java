package com.part3_team4.deokhoogam.domain.comment.exception;

import com.part3_team4.deokhoogam.global.exception.ErrorCode;

public class CommentContentTooLongException extends CommentException {

    private CommentContentTooLongException() {
        super(ErrorCode.COMMENT_CONTENT_TOO_LONG);
    }

    public static CommentContentTooLongException create() {
        return new CommentContentTooLongException();
    }
}