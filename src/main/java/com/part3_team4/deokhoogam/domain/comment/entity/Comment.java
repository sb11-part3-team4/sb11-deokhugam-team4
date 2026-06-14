package com.part3_team4.deokhoogam.domain.comment.entity;

import com.part3_team4.deokhoogam.domain.comment.exception.CommentContentRequiredException;
import com.part3_team4.deokhoogam.domain.comment.exception.CommentContentTooLongException;
import com.part3_team4.deokhoogam.domain.comment.exception.CommentInvalidArgumentException;
import com.part3_team4.deokhoogam.domain.review.entity.Review;
import com.part3_team4.deokhoogam.domain.user.entity.User;
import com.part3_team4.deokhoogam.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "comment", indexes = {
    @Index(name = "idx_comment_review_created", columnList = "review_id, created_at DESC")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseEntity {

  public static final int MAX_CONTENT_LENGTH = 1000;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "review_id", nullable = false)
  private Review review;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false, length = MAX_CONTENT_LENGTH)
  private String content;

  private Comment(Review review, User user, String content) {
    this.review = review;
    this.user = user;
    this.content = content;
  }

  public static Comment create(Review review, User user, String content) {
    if (review == null || user == null) {
      throw CommentInvalidArgumentException.create();
    }
    validate(content);
    return new Comment(review, user, content);
  }

  public UUID getReviewId() {
    return review.getId();
  }

  public UUID getUserId() {
    return user.getId();
  }

  public void updateContent(String content) {
    validate(content);
    this.content = content;
  }

  private static void validate(String content) {
    if (content == null || content.isBlank()) {
      throw CommentContentRequiredException.create();
    }
    if (content.length() > MAX_CONTENT_LENGTH) {
      throw CommentContentTooLongException.create();
    }
  }
}