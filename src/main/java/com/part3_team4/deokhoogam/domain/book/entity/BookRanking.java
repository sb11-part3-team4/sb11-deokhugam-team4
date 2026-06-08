package com.part3_team4.deokhoogam.domain.book.entity;

import com.part3_team4.deokhoogam.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "book_ranking", uniqueConstraints = {
    @UniqueConstraint(name = "uk_book_ranking_book_period", columnNames = {"book_id", "period"})
}, indexes = {
    @Index(name = "idx_book_ranking_period_ranking", columnList = "period, ranking")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class BookRanking extends BaseEntity {

  @Column(name = "book_id", nullable = false)
  private UUID bookId;

  @Enumerated(EnumType.STRING)
  @Column(name = "period", nullable = false, length = 20)
  private PeriodType period;

  @Column(name = "score", nullable = false, precision = 10, scale = 4)
  private BigDecimal score;

  @Column(name = "ranking", nullable = false)
  private int ranking;

  @Column(name = "period_review_count", nullable = false)
  private long reviewCount;

  @Column(name = "period_rating", nullable = false, precision = 3, scale = 2)
  private BigDecimal rating;

  @Builder
  public BookRanking(UUID bookId, PeriodType period, BigDecimal score, int ranking,
      long reviewCount, BigDecimal rating) {
    this.bookId = bookId;
    this.period = period;
    this.score = score;
    this.ranking = ranking;
    this.reviewCount = reviewCount;
    this.rating = rating;
  }
}