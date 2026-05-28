package com.part3_team4.deokhoogam.domain.comment.repository;

import com.part3_team4.deokhoogam.domain.comment.entity.Comment;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    List<Comment> findByReviewIdOrderByCreatedAtDesc(UUID reviewId, Pageable pageable);

    List<Comment> findByReviewIdAndCreatedAtBeforeOrderByCreatedAtDesc(UUID reviewId, Instant before, Pageable pageable);

    void deleteByReviewId(UUID reviewId);

    long countByReviewId(UUID reviewId);

    long countByUserId(UUID userId);
}