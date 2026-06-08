package com.part3_team4.deokhoogam.domain.book.dto.ranking;

import java.math.BigDecimal;
import java.util.UUID;

public interface BookScoreProjection { // 네이티브 쿼리용
  UUID getBookId();
  long getReviewCount();
  BigDecimal getAvgRating();
}