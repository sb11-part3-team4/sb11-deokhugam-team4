package com.part3_team4.deokhoogam.domain.comment.dto;

import com.part3_team4.deokhoogam.domain.comment.entity.Comment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class CommentDto {

    public record CreateCommentRequest(
            @NotNull UUID reviewId,
            UUID userId,
            @NotBlank String content
    ) {}

    public record CommentsResponse(
            List<CommentResponse> content,
            String nextCursor,
            Instant nextAfter,
            int size,
            long totalElements,
            boolean hasNext
    ) {}

    public record UpdateCommentRequest(
            @NotBlank String content
    ) {}

    public record CommentResponse(
            UUID id,
            UUID reviewId,
            UUID userId,
            String userNickname,
            String content,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static CommentResponse from(Comment comment, String userNickname) {
            return new CommentResponse(
                    comment.getId(),
                    comment.getReviewId(),
                    comment.getUserId(),
                    userNickname,
                    comment.getContent(),
                    comment.getCreatedAt(),
                    comment.getUpdatedAt()
            );
        }
    }
}
