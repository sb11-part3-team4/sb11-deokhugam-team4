package com.part3_team4.deokhoogam.domain.review.dto;

import com.part3_team4.deokhoogam.domain.review.entity.Review;

public record ReviewWithLiked(
    Review review,
    boolean likedByMe
) {}
