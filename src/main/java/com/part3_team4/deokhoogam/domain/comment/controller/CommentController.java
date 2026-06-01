package com.part3_team4.deokhoogam.domain.comment.controller;

import com.part3_team4.deokhoogam.domain.comment.dto.CommentDto;
import com.part3_team4.deokhoogam.domain.comment.service.CommentService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private static final String USER_HEADER = "Deokhugam-Request-User-ID";

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<CommentDto.CommentResponse> createComment(
            // TODO: [GlobalExceptionHandler] MissingRequestHeaderException 핸들러 추가 후 required=false → required=true로 교체
            @RequestHeader(value = USER_HEADER, required = false) UUID userId,
            @RequestBody @Valid CommentDto.CreateCommentRequest request) {
        UUID effectiveUserId = (userId != null) ? userId : request.userId();
        if (effectiveUserId == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.createComment(request.reviewId(), effectiveUserId, request.content()));
    }

    @GetMapping
    public ResponseEntity<CommentDto.CommentsResponse> getComments(
            @RequestParam UUID reviewId,
            @RequestParam(defaultValue = "DESC") String direction,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Instant after,
            @RequestParam(defaultValue = "50") int limit) {
        Instant cursorInstant = null;
        if (cursor != null) {
            try {
                cursorInstant = Instant.parse(cursor);
            } catch (DateTimeParseException e) {
                // TODO: [GlobalExceptionHandler] DateTimeParseException 핸들러 추가 후 try-catch 제거
                return ResponseEntity.badRequest().build();
            }
        }
        return ResponseEntity.ok(commentService.getComments(reviewId, direction, cursorInstant, after, limit));
    }

    @GetMapping("/{commentId}")
    public ResponseEntity<CommentDto.CommentResponse> getComment(
            @PathVariable UUID commentId) {
        return ResponseEntity.ok(commentService.getComment(commentId));
    }

    @PatchMapping("/{commentId}")
    public ResponseEntity<CommentDto.CommentResponse> updateComment(
            @PathVariable UUID commentId,
            // TODO: [GlobalExceptionHandler] MissingRequestHeaderException 핸들러 추가 후 required=false → required=true로 교체
            @RequestHeader(value = USER_HEADER, required = false) UUID userId,
            @RequestBody @Valid CommentDto.UpdateCommentRequest request) {
        if (userId == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return ResponseEntity.ok(commentService.updateComment(commentId, userId, request.content()));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> softDeleteComment(
            @PathVariable UUID commentId,
            // TODO: [GlobalExceptionHandler] MissingRequestHeaderException 핸들러 추가 후 required=false → required=true로 교체
            @RequestHeader(value = USER_HEADER, required = false) UUID userId) {
        if (userId == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        commentService.softDeleteComment(commentId, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{commentId}/hard")
    public ResponseEntity<Void> hardDeleteComment(
            @PathVariable UUID commentId,
            // TODO: [GlobalExceptionHandler] MissingRequestHeaderException 핸들러 추가 후 required=false → required=true로 교체
            @RequestHeader(value = USER_HEADER, required = false) UUID userId) {
        if (userId == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        commentService.hardDeleteComment(commentId, userId);
        return ResponseEntity.noContent().build();
    }
}
