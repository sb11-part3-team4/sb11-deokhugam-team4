package com.part3_team4.deokhoogam.domain.comment.service;

import com.part3_team4.deokhoogam.domain.comment.dto.CommentDto;
import com.part3_team4.deokhoogam.domain.comment.entity.Comment;
import com.part3_team4.deokhoogam.domain.comment.entity.DeletedComment;
import com.part3_team4.deokhoogam.domain.comment.exception.CommentNotFoundException;
import com.part3_team4.deokhoogam.domain.comment.exception.CommentNotOwnerException;
import com.part3_team4.deokhoogam.domain.comment.repository.CommentRepository;
import com.part3_team4.deokhoogam.domain.comment.repository.DeletedCommentRepository;
import com.part3_team4.deokhoogam.domain.review.repository.ReviewRepository;
import com.part3_team4.deokhoogam.domain.user.entity.User;
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
        // TODO: createComment에서도 userNickname 채우기 필요 - User 도메인 연결 후 구현
        return CommentDto.CommentResponse.from(saved, null);
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
        // TODO: updateComment에서도 userNickname 채우기 필요 - User 도메인 연결 후 구현
        return CommentDto.CommentResponse.from(comment, null);
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
    public void hardDeleteComment(UUID commentId, UUID userId) {
        DeletedComment deletedComment = deletedCommentRepository.findById(commentId)
                .orElseThrow(() -> CommentNotFoundException.withId(commentId));
        if (!deletedComment.getUserId().equals(userId)) {
            throw CommentNotOwnerException.forUser(userId);
        }
        deletedCommentRepository.delete(deletedComment);
    }

    @Override
    public CommentDto.CommentsResponse getComments(UUID reviewId, String direction, Instant cursor, Instant after, int limit) {
        if (!reviewRepository.existsById(reviewId)) {
            // TODO: Review 도메인 팀 작업 후 ReviewNotFoundException.withId(reviewId)로 교체
            throw new IllegalArgumentException("존재하지 않는 리뷰입니다.");
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
        // TODO: N+1 문제 개선 필요 - userId를 일괄 수집 후 배치 조회로 전환
        List<CommentDto.CommentResponse> content = comments.stream().map(comment -> {
            // TODO: User 조회 실패 시 처리 방식 팀 협의 필요 (현재: null 반환)
            String userNickname = userRepository.findById(comment.getUserId())
                    .map(User::getName).orElse(null);
            return CommentDto.CommentResponse.from(comment, userNickname);
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
        // TODO: User 조회 실패 시 처리 방식 팀 협의 필요 (현재: null 반환)
        String userNickname = userRepository.findById(comment.getUserId())
                .map(User::getName).orElse(null);
        return CommentDto.CommentResponse.from(comment, userNickname);
    }
}
