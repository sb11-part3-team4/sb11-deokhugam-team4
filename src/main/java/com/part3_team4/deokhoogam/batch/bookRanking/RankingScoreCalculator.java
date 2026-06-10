package com.part3_team4.deokhoogam.batch.bookRanking;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class RankingScoreCalculator {

  private static final BigDecimal REVIEW_WEIGHT = new BigDecimal("0.4");
  private static final BigDecimal RATING_WEIGHT = new BigDecimal("0.6");

  public BigDecimal calculate(long reviewCount, BigDecimal avgRating) {
    BigDecimal rating = (avgRating == null) ? BigDecimal.ZERO : avgRating;
    BigDecimal reviewPart = BigDecimal.valueOf(reviewCount).multiply(REVIEW_WEIGHT);
    BigDecimal ratingPart = rating.multiply(RATING_WEIGHT);
    return reviewPart.add(ratingPart).setScale(4, RoundingMode.HALF_UP);
  }
}

