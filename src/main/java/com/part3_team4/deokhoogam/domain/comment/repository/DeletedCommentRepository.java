package com.part3_team4.deokhoogam.domain.comment.repository;

import com.part3_team4.deokhoogam.domain.comment.entity.DeletedComment;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeletedCommentRepository extends JpaRepository<DeletedComment, UUID> {

    void deleteByReviewId(UUID reviewId);

    long countByReviewId(UUID reviewId);

    long countByUserId(UUID userId);
}