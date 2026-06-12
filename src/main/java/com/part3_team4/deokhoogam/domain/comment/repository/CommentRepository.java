package com.part3_team4.deokhoogam.domain.comment.repository;

import com.part3_team4.deokhoogam.domain.comment.entity.Comment;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    // TODO: 커서를 (createdAt, id) 복합키로 전환하고 정렬을 createdAt DESC, id DESC로 고도화
    //       현재 createdAt 단일 정렬은 페이지 경계에서 동일 타임스탬프 레코드를 영구 누락시킬 수 있음
    @Query("SELECT c FROM Comment c WHERE c.review.id = :reviewId ORDER BY c.createdAt DESC")
    List<Comment> findByReviewIdOrderByCreatedAtDesc(@Param("reviewId") UUID reviewId, Pageable pageable);

    // TODO: 커서 조건을 (createdAt, id) 복합키 기반(createdAt < cursor OR (createdAt = cursor AND id < cursorId))으로 전환
    @Query("SELECT c FROM Comment c WHERE c.review.id = :reviewId AND c.createdAt < :before ORDER BY c.createdAt DESC")
    List<Comment> findByReviewIdAndCreatedAtBeforeOrderByCreatedAtDesc(@Param("reviewId") UUID reviewId, @Param("before") Instant before, Pageable pageable);

    @Query("SELECT c FROM Comment c WHERE c.review.id = :reviewId ORDER BY c.createdAt ASC")
    List<Comment> findByReviewIdOrderByCreatedAtAsc(@Param("reviewId") UUID reviewId, Pageable pageable);

    @Query("SELECT c FROM Comment c WHERE c.review.id = :reviewId AND c.createdAt > :after ORDER BY c.createdAt ASC")
    List<Comment> findByReviewIdAndCreatedAtAfterOrderByCreatedAtAsc(@Param("reviewId") UUID reviewId, @Param("after") Instant after, Pageable pageable);

    @Modifying
    @Transactional
    @Query("DELETE FROM Comment c WHERE c.review.id = :reviewId")
    void deleteByReviewId(@Param("reviewId") UUID reviewId);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.review.id = :reviewId")
    long countByReviewId(@Param("reviewId") UUID reviewId);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.user.id = :userId")
    long countByUserId(@Param("userId") UUID userId);

    // 이벤트 리스너용 다건 조회 메서드 추가
    @Query("SELECT c FROM Comment c WHERE c.user.id = :userId")
    List<Comment> findAllByUserId(@Param("userId") UUID userId);
    @Query("SELECT c FROM Comment c WHERE c.review.id = :reviewId")
    List<Comment> findAllByReviewId(@Param("reviewId") UUID reviewId);
}