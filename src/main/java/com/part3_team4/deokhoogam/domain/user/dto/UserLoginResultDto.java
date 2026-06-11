package com.part3_team4.deokhoogam.domain.user.dto;

import java.time.Instant;
import java.util.UUID;

public record UserLoginResultDto(
    UUID id,
    String email,
    String nickname,
    Instant createdAt
) {

}
