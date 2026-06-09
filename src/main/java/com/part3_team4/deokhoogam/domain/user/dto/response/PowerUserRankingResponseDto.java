package com.part3_team4.deokhoogam.domain.user.dto.response;

import java.util.UUID;

public record PowerUserRankingResponseDto(
    UUID userId,
    String nickname,
    int rank,
    double score
) {

}
