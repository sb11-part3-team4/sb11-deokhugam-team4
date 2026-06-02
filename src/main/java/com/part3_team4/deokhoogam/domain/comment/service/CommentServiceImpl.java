package com.part3_team4.deokhoogam.domain.comment.service;

import com.part3_team4.deokhoogam.domain.comment.dto.CommentDto;
import com.part3_team4.deokhoogam.domain.comment.entity.Comment;
import com.part3_team4.deokhoogam.domain.comment.entity.DeletedComment;
import com.part3_team4.deokhoogam.domain.comment.exception.CommentNotFoundException;
import com.part3_team4.deokhoogam.domain.comment.exception.CommentNotOwnerException;
import com.part3_team4.deokhoogam.domain.comment.repository.CommentRepository;
import com.part3_team4.deokhoogam.domain.comment.repository.DeletedCommentRepository;
import com.part3_team4.deokhoogam.domain.review.entity.Review;
import com.part3_team4.deokhoogam.domain.review.exception.ReviewNotFoundException;
import com.part3_team4.deokhoogam.domain.review.repository.ReviewRepository;
import com.part3_team4.deokhoogam.domain.user.entity.User;
import com.part3_team4.deokhoogam.domain.notification.service.NotificationService;
import com.part3_team4.deokhoogam.domain.user.exception.UserNotFoundException;
import com.part3_team4.deokhoogam.domain.user.repository.UserRepository;
import java.time.Instant;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final DeletedCommentRepository deletedCommentRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public CommentDto.CommentResponse createComment(UUID reviewId, UUID userId, String content) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> ReviewNotFoundException.withId(reviewId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.withId(userId));
        Comment saved = commentRepository.save(Comment.create(review, user, content));
        try {
            notificationService.createNotification(
                    review.getUserId(),
                    review.getId(),
                    review.getContent(),
                    user.getName(),
                    user.getId()
            );
        } catch (Exception e) {
            log.warn("알림 생성 실패 - reviewId: {}, actorId: {}", review.getId(), user.getId(), e);
        }
        return CommentDto.CommentResponse.from(saved, user.getName());
    }

    @Override
    @Transactional
    public CommentDto.CommentResponse updateComment(UUID commentId, UUID userId, String content) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> CommentNotFoundException.withId(commentId));
        if (!comment.getUserId().equals(userId)) {
            throw CommentNotOwnerException.forUser(userId);
        }
        comment.updateContent(content);
        return CommentDto.CommentResponse.from(comment, comment.getUser().getName());
    }

    @Override
    @Transactional
    public void softDeleteComment(UUID commentId, UUID userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> CommentNotFoundException.withId(commentId));
        if (!comment.getUserId().equals(userId)) {
            throw CommentNotOwnerException.forUser(userId);
        }
        // deleted_comment 먼저 저장 후 원본 삭제 (반대 순서면 참조 무결성 문제)
        deletedCommentRepository.save(DeletedComment.from(comment));
        commentRepository.delete(comment);
    }

    @Override
    @Transactional
    public void hardDeleteComment(UUID commentId) {
        DeletedComment deletedComment = deletedCommentRepository.findById(commentId)
                .orElseThrow(() -> CommentNotFoundException.withId(commentId));
        deletedCommentRepository.delete(deletedComment);
    }

    @Override
    public CommentDto.CommentsResponse getComments(UUID reviewId, String direction, Instant cursor, Instant after, int limit) {
        if (!reviewRepository.existsById(reviewId)) {
            throw ReviewNotFoundException.withId(reviewId);
        }
        PageRequest pageable = PageRequest.of(0, limit);
        boolean isAsc = "ASC".equalsIgnoreCase(direction);
        List<Comment> comments;
        if (isAsc) {
            comments = (after == null)
                    ? commentRepository.findByReviewIdOrderByCreatedAtAsc(reviewId, pageable)
                    : commentRepository.findByReviewIdAndCreatedAtAfterOrderByCreatedAtAsc(reviewId, after, pageable);
        } else {
            comments = (cursor == null)
                    ? commentRepository.findByReviewIdOrderByCreatedAtDesc(reviewId, pageable)
                    : commentRepository.findByReviewIdAndCreatedAtBeforeOrderByCreatedAtDesc(reviewId, cursor, pageable);
        }

        Set<UUID> userIds = comments.stream().map(Comment::getUserId).collect(Collectors.toSet());
        Map<UUID, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(u -> u.getId(), u -> u));

        List<CommentDto.CommentResponse> content = comments.stream().map(comment -> {
            User commentUser = userMap.get(comment.getUserId());
            if (commentUser == null) {
                throw UserNotFoundException.withId(comment.getUserId());
            }
            return CommentDto.CommentResponse.from(comment, commentUser.getName());
        }).toList();

        int size = content.size();
        boolean hasNext = size == limit;
        long totalElements = commentRepository.countByReviewId(reviewId);

        String nextCursor = null;
        Instant nextAfter = null;
        if (hasNext) {
            Instant lastCreatedAt = comments.get(size - 1).getCreatedAt();
            nextCursor = lastCreatedAt.toString();
            nextAfter = lastCreatedAt;
        }

        return new CommentDto.CommentsResponse(content, nextCursor, nextAfter, size, totalElements, hasNext);
    }

    @Override
    public CommentDto.CommentResponse getComment(UUID commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> CommentNotFoundException.withId(commentId));
        return CommentDto.CommentResponse.from(comment, comment.getUser().getName());
    }
}