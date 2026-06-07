package com.part3_team4.deokhoogam.domain.book.dto;

import java.util.List;

public record NaverApiResponse(
    Integer total,
    List<NaverBookDto> items
) {}
