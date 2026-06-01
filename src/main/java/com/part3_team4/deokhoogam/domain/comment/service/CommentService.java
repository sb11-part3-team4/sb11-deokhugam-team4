package com.part3_team4.deokhoogam.domain.comment.service;

import com.part3_team4.deokhoogam.domain.comment.dto.CommentDto;
import java.time.Instant;
import java.util.UUID;

public interface CommentService {

    CommentDto.CommentResponse createComment(UUID reviewId, UUID userId, String content);

    CommentDto.CommentResponse updateComment(UUID commentId, UUID userId, String content);

    void softDeleteComment(UUID commentId, UUID userId);

    void hardDeleteComment(UUID commentId, UUID userId);

    CommentDto.CommentsResponse getComments(UUID reviewId, String direction, Instant cursor, Instant after, int limit);

    CommentDto.CommentResponse getComment(UUID commentId);
}
