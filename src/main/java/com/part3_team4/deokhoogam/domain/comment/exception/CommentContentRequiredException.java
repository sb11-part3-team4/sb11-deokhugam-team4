package com.part3_team4.deokhoogam.domain.comment.exception;

import com.part3_team4.deokhoogam.global.exception.ErrorCode;

public class CommentContentRequiredException extends CommentException {

    private CommentContentRequiredException() {
        super(ErrorCode.COMMENT_CONTENT_REQUIRED);
    }

    public static CommentContentRequiredException create() {
        return new CommentContentRequiredException();
    }
}