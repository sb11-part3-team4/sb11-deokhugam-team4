package com.part3_team4.deokhoogam.domain.comment.repository;

import com.part3_team4.deokhoogam.domain.comment.entity.DeletedComment;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeletedCommentRepository extends JpaRepository<DeletedComment, UUID> {

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM DeletedComment dc WHERE dc.reviewId = :reviewId")
    void deleteByReviewId(@Param("reviewId") UUID reviewId);

    // 물리 삭제용 유저 ID로 백업 댓글 일괄 삭제 메서드 추가
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM DeletedComment dc WHERE dc.userId = :userId")
    void deleteByUserId(@Param("userId") UUID userId);

    long countByReviewId(UUID reviewId);

    long countByUserId(UUID userId);
}