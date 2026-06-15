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

    @Query("SELECT c FROM Comment c WHERE c.review.id = :reviewId ORDER BY c.createdAt DESC, c.id DESC")
    List<Comment> findByReviewIdOrderByCreatedAtDesc(@Param("reviewId") UUID reviewId, Pageable pageable);

    @Query("SELECT c FROM Comment c WHERE c.review.id = :reviewId ORDER BY c.createdAt ASC, c.id ASC")
    List<Comment> findByReviewIdOrderByCreatedAtAsc(@Param("reviewId") UUID reviewId, Pageable pageable);

    @Query("""
        SELECT c FROM Comment c
        WHERE c.review.id = :reviewId
          AND (
            c.createdAt < :after
            OR (c.createdAt = :after AND c.id < :cursor)
          )
        ORDER BY c.createdAt DESC, c.id DESC
        """)
    List<Comment> findNextPageDesc(
        @Param("reviewId") UUID reviewId,
        @Param("after") Instant after,
        @Param("cursor") UUID cursor,
        Pageable pageable
    );

    @Query("""
        SELECT c FROM Comment c
        WHERE c.review.id = :reviewId
          AND (
            c.createdAt > :after
            OR (c.createdAt = :after AND c.id > :cursor)
          )
        ORDER BY c.createdAt ASC, c.id ASC
        """)
    List<Comment> findNextPageAsc(
        @Param("reviewId") UUID reviewId,
        @Param("after") Instant after,
        @Param("cursor") UUID cursor,
        Pageable pageable
    );

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