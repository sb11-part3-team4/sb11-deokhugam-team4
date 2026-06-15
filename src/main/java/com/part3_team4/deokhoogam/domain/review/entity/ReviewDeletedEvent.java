package com.part3_team4.deokhoogam.domain.review.entity;

import java.util.UUID;

public record ReviewDeletedEvent(
    UUID reviewId,
    boolean isHardDelete //논리 삭제(false), 물리 삭제(true)
) {

}
