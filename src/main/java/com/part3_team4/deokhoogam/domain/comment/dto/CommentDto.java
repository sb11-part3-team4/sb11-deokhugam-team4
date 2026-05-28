package com.part3_team4.deokhoogam.domain.comment.dto;

import com.part3_team4.deokhoogam.domain.comment.entity.Comment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public class CommentDto {

    public record CreateCommentRequest(
            @NotNull UUID reviewId,
            @NotBlank String content
    ) {}

    public record UpdateCommentRequest(
            @NotBlank String content
    ) {}

    public record CommentResponse(
            UUID id,
            UUID reviewId,
            UUID userId,
            String content,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static CommentResponse from(Comment comment) {
            return new CommentResponse(
                    comment.getId(),
                    comment.getReviewId(),
                    comment.getUserId(),
                    comment.getContent(),
                    comment.getCreatedAt(),
                    comment.getUpdatedAt()
            );
        }
    }
}
