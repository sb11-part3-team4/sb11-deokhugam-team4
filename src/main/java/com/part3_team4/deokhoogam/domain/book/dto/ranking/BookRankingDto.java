package com.part3_team4.deokhoogam.domain.book.dto.ranking;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BookRankingDto(
    UUID id,
    UUID bookId,
    String title,
    String author,
    String thumbnailUrl,
    String period,
    int rank,
    BigDecimal score,
    long reviewCount,
    BigDecimal rating,
    Instant createdAt
) {}