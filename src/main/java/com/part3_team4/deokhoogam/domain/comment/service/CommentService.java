package com.part3_team4.deokhoogam.domain.comment.service;

import com.part3_team4.deokhoogam.domain.comment.dto.CommentDto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface CommentService {

    CommentDto.CommentResponse createComment(UUID reviewId, UUID userId, String content);

    CommentDto.CommentResponse updateComment(UUID commentId, UUID userId, String content);

    void softDeleteComment(UUID commentId, UUID userId);

    void hardDeleteComment(UUID commentId);

    List<CommentDto.CommentResponse> getComments(UUID reviewId, Instant cursor, int limit);

    CommentDto.CommentResponse getComment(UUID commentId);
}
