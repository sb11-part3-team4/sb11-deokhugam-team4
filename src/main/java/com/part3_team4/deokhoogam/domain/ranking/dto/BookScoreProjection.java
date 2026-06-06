package com.part3_team4.deokhoogam.domain.ranking.dto;

import java.math.BigDecimal;
import java.util.UUID;

public interface BookScoreProjection {
  UUID getBookId();
  long getReviewCount();
  BigDecimal getAvgRating();
}