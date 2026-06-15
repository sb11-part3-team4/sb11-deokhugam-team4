package com.part3_team4.deokhoogam.domain.review.entity;

import com.part3_team4.deokhoogam.domain.review.exception.InvalidReviewException;
import com.part3_team4.deokhoogam.global.common.BaseEntity;
import com.part3_team4.deokhoogam.global.exception.ErrorKey;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "review", indexes = {
    @Index(name = "idx_review_cursor_created_at", columnList = "book_id, created_at DESC, id DESC"),
    @Index(name = "idx_review_cursor_rating", columnList = "book_id, rating DESC, created_at DESC, id DESC"),
    @Index(name = "idx_review_list_created_at", columnList = "created_at DESC, id DESC")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseEntity {

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "book_id", nullable = false)
  private UUID bookId;

  @Column(nullable = false)
  private int rating;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name = "like_count", nullable = false)
  private int likeCount;

  @Column(name = "comment_count", nullable = false)
  private int commentCount;

  public static Review create(UUID userId, UUID bookId, int rating, String content) {
    validate(rating, content);

    Review review = new Review();
    review.userId = userId;
    review.bookId = bookId;
    review.rating = rating;
    review.content = content;
    review.likeCount = 0;
    review.commentCount = 0;
    return review;
  }

  public void update(int rating, String content) {
    validate(rating, content);
    this.rating = rating;
    this.content = content;
  }

  private static void validate(int rating, String content) {
    if (rating < 1 || rating > 5) {
      throw InvalidReviewException.withFieldAndValue(ErrorKey.REVIEW_RATING, rating,
          "rating은 1~5 사이여야 합니다.");
    }
    if (content == null || content.isBlank()) {
      throw InvalidReviewException.withField(ErrorKey.REVIEW_CONTENT, "content는 필수입니다.");
    }
  }

  public void incrementLikeCount() {
    this.likeCount++;
  }

  public void decrementLikeCount() {
    this.likeCount--;
  }

  public void incrementCommentCount() {
    this.commentCount++;
  }

  public void decrementCommentCount() {
      if (this.commentCount > 0) {
          this.commentCount--;
      }
  }
}
