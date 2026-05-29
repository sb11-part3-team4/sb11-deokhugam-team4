package com.part3_team4.deokhoogam.domain.review.dto;

import java.util.UUID;

public record ReviewLikeResponse (
    UUID reviewId,
    boolean liked,
    int likeCount
) {}
