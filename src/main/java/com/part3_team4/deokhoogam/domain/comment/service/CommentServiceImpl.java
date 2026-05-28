package com.part3_team4.deokhoogam.domain.comment.service;

import com.part3_team4.deokhoogam.domain.comment.dto.CommentDto;
import com.part3_team4.deokhoogam.domain.comment.entity.Comment;
import com.part3_team4.deokhoogam.domain.comment.entity.DeletedComment;
import com.part3_team4.deokhoogam.domain.comment.exception.CommentNotFoundException;
import com.part3_team4.deokhoogam.domain.comment.exception.CommentNotOwnerException;
import com.part3_team4.deokhoogam.domain.comment.repository.CommentRepository;
import com.part3_team4.deokhoogam.domain.comment.repository.DeletedCommentRepository;
import com.part3_team4.deokhoogam.domain.review.repository.ReviewRepository;
import com.part3_team4.deokhoogam.domain.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final DeletedCommentRepository deletedCommentRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    @Override
    @Transactional
    public CommentDto.CommentResponse createComment(UUID reviewId, UUID userId, String content) {
        if (!reviewRepository.existsById(reviewId)) {
            // TODO: Review 도메인 팀 작업 후 ReviewNotFoundException.withId(reviewId)로 교체
            throw new IllegalArgumentException("존재하지 않는 리뷰입니다.");
        }
        if (!userRepository.existsById(userId)) {
            // TODO: User 도메인 팀 작업 후 UserNotFoundException.withId(userId)로 교체
            throw new IllegalArgumentException("존재하지 않는 유저입니다.");
        }
        Comment saved = commentRepository.save(Comment.create(reviewId, userId, content));
        // TODO: 알림 담당 팀원 코드 연결 후 구현
        return CommentDto.CommentResponse.from(saved);
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
        return CommentDto.CommentResponse.from(comment);
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
    public List<CommentDto.CommentResponse> getComments(UUID reviewId, Instant cursor, int limit) {
        if (!reviewRepository.existsById(reviewId)) {
            // TODO: Review 도메인 팀 작업 후 ReviewNotFoundException.withId(reviewId)로 교체
            throw new IllegalArgumentException("존재하지 않는 리뷰입니다.");
        }
        PageRequest pageable = PageRequest.of(0, limit);
        List<Comment> comments = (cursor == null)
                ? commentRepository.findByReviewIdOrderByCreatedAtDesc(reviewId, pageable)
                : commentRepository.findByReviewIdAndCreatedAtBeforeOrderByCreatedAtDesc(reviewId, cursor, pageable);
        return comments.stream().map(CommentDto.CommentResponse::from).toList();
    }

    @Override
    public CommentDto.CommentResponse getComment(UUID commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> CommentNotFoundException.withId(commentId));
        return CommentDto.CommentResponse.from(comment);
    }
}
