package com.part3_team4.deokhoogam.domain.comment.exception;

import com.part3_team4.deokhoogam.global.exception.ErrorCode;

public class CommentInvalidArgumentException extends CommentException {

    private CommentInvalidArgumentException() {
        super(ErrorCode.INVALID_INPUT_VALUE);
    }

    public static CommentInvalidArgumentException create() {
        return new CommentInvalidArgumentException();
    }
}