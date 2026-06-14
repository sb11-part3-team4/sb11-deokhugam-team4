package com.part3_team4.deokhoogam.domain.review.repository;

import com.part3_team4.deokhoogam.domain.review.dto.ReviewWithLiked;
import com.part3_team4.deokhoogam.domain.review.entity.Review;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

  @Query("""
      SELECT r FROM Review r
      WHERE (:userId IS NULL OR r.userId = :userId)
      AND (:bookId IS NULL OR r.bookId = :bookId)
      AND (:keyword IS NULL OR r.content LIKE CONCAT('%', CAST(:keyword AS String), '%'))
      """)
  List<Review> findReviews(
      @Param("userId") UUID userId,
      @Param("bookId") UUID bookId,
      @Param("keyword") String keyword,
      Pageable pageable
  );

  boolean existsByUserIdAndBookId(UUID userId, UUID bookId);

  long countByBookId(UUID bookId);

  @Query("SELECT COALESCE(AVG(r.rating), 0) FROM Review r WHERE r.bookId = :bookId")
  BigDecimal averageRatingByBookId(@Param("bookId") UUID bookId);

  @Query("""
          SELECT new com.part3_team4.deokhoogam.domain.review.dto.ReviewWithLiked(
          r, CASE WHEN rl.id IS NOT NULL THEN true ELSE false END
          )
          FROM Review r
          LEFT JOIN ReviewLike rl ON rl.reviewId = r.id AND rl.userId = :userId
          WHERE r.id = :reviewId
  """)
  Optional<ReviewWithLiked> findByIdWithLiked(
      @Param("reviewId") UUID reviewId,
      @Param("userId") UUID userId
  );

  // 이벤트 리스너용 다건 조회 메서드
  List<Review> findAllByUserId(UUID userId);
  List<Review> findAllByBookId(UUID bookId);

  @Modifying
  @Query("UPDATE Review r SET r.commentCount = r.commentCount + 1 WHERE r.id = :reviewId")
  int incrementCommentCount(@Param("reviewId") UUID reviewId);

  @Modifying
  @Query("UPDATE Review r SET r.commentCount = r.commentCount - 1 WHERE r.id = :reviewId AND r.commentCount > 0")
  int decrementCommentCount(@Param("reviewId") UUID reviewId);

  @Modifying(clearAutomatically = true)
  @Query("UPDATE Review r SET r.likeCount = r.likeCount + 1 WHERE r.id = :reviewId")
  void incrementLikeCount(@Param("reviewId") UUID reviewId);

  @Modifying(clearAutomatically = true)
  @Query("UPDATE Review r SET r.likeCount = r.likeCount - 1 WHERE r.id = :reviewId AND r.likeCount > 0")
  void decrementLikeCount(@Param("reviewId") UUID reviewId);

  // 원자적 업데이트 후 클라이언트에게 내려줄 최신 카운트 단건 조회용
  @Query("SELECT r.likeCount FROM Review r WHERE r.id = :reviewId")
  int getLikeCount(@Param("reviewId") UUID reviewId);

  @Query("SELECT r FROM Review r WHERE r.createdAt >= :start AND r.createdAt <= :end")
  List<Review> findByCreatedAtBetween(@Param("start") Instant start, @Param("end") Instant end);

}