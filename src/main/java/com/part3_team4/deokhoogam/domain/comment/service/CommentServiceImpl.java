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
import com.part3_team4.deokhoogam.domain.review.service.ReviewService;
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
    private final ReviewService reviewService;

    @Override
    @Transactional
    public CommentDto.CommentResponse createComment(UUID reviewId, UUID userId, String content) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> ReviewNotFoundException.withId(reviewId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.withId(userId));
        Comment saved = commentRepository.save(Comment.create(review, user, content));
        notificationService.createNotification(
                review.getUserId(),
                review.getId(),
                review.getContent(),
                user.getName(),
                user.getId()
        );
        reviewService.incrementCommentCount(reviewId);
        log.info("댓글 생성 완료 - commentId: {}, reviewId: {}, userId: {}", saved.getId(), reviewId, userId);
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
        log.info("댓글 수정 완료 - commentId: {}", commentId);
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
        UUID reviewId = comment.getReviewId();
        // deleted_comment 먼저 저장 후 원본 삭제 (반대 순서면 참조 무결성 문제)
        deletedCommentRepository.save(DeletedComment.from(comment));
        commentRepository.delete(comment);
        reviewService.decrementCommentCount(reviewId);
        log.info("댓글 삭제(soft) 완료 - commentId: {}, reviewId: {}", commentId, reviewId);
    }

    @Override
    @Transactional
    public void hardDeleteComment(UUID commentId) {
        DeletedComment deletedComment = deletedCommentRepository.findById(commentId)
                .orElseThrow(() -> CommentNotFoundException.withId(commentId));
        deletedCommentRepository.delete(deletedComment);
        log.info("댓글 영구 삭제 완료 - commentId: {}", commentId);
    }

    @Override
    public CommentDto.CommentsResponse getComments(UUID reviewId, String direction, UUID cursor, Instant after, int limit) {
        if (!reviewRepository.existsById(reviewId)) {
            throw ReviewNotFoundException.withId(reviewId);
        }
        PageRequest pageable = PageRequest.of(0, limit + 1);
        boolean isAsc = "ASC".equalsIgnoreCase(direction);
        List<Comment> comments;
        if (cursor == null && after == null) {
            comments = isAsc
                    ? commentRepository.findByReviewIdOrderByCreatedAtAsc(reviewId, pageable)
                    : commentRepository.findByReviewIdOrderByCreatedAtDesc(reviewId, pageable);
        } else {
            comments = isAsc
                    ? commentRepository.findNextPageAsc(reviewId, after, cursor, pageable)
                    : commentRepository.findNextPageDesc(reviewId, after, cursor, pageable);
        }

        boolean hasNext = comments.size() > limit;
        List<Comment> pagedComments = hasNext ? comments.subList(0, limit) : comments;

        Set<UUID> userIds = pagedComments.stream().map(Comment::getUserId).collect(Collectors.toSet());
        Map<UUID, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(u -> u.getId(), u -> u));

        List<CommentDto.CommentResponse> content = pagedComments.stream().map(comment -> {
            User commentUser = userMap.get(comment.getUserId());
            if (commentUser == null) {
                throw UserNotFoundException.withId(comment.getUserId());
            }
            return CommentDto.CommentResponse.from(comment, commentUser.getName());
        }).toList();

        int size = content.size();
        long totalElements = commentRepository.countByReviewId(reviewId);

        String nextCursor = null;
        Instant nextAfter = null;
        if (hasNext) {
            Comment last = pagedComments.get(size - 1);
            nextCursor = last.getId().toString();
            nextAfter = last.getCreatedAt();
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