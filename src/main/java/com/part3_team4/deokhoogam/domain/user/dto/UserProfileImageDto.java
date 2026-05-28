package com.part3_team4.deokhoogam.domain.user.dto;

import java.util.UUID;

public record UserProfileImageDto(
    UUID id,
    String fileName,
    Long size,
    String contentType
) {

}
