package com.part3_team4.deokhoogam.domain.comment.repository;

import com.part3_team4.deokhoogam.domain.comment.entity.Comment;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    // TODO: 커서를 (createdAt, id) 복합키로 전환하고 정렬을 createdAt DESC, id DESC로 고도화
    //       현재 createdAt 단일 정렬은 페이지 경계에서 동일 타임스탬프 레코드를 영구 누락시킬 수 있음
    List<Comment> findByReviewIdOrderByCreatedAtDesc(UUID reviewId, Pageable pageable);

    // TODO: 커서 조건을 (createdAt, id) 복합키 기반(createdAt < cursor OR (createdAt = cursor AND id < cursorId))으로 전환
    List<Comment> findByReviewIdAndCreatedAtBeforeOrderByCreatedAtDesc(UUID reviewId, Instant before, Pageable pageable);

    void deleteByReviewId(UUID reviewId);

    long countByReviewId(UUID reviewId);

    long countByUserId(UUID userId);
}