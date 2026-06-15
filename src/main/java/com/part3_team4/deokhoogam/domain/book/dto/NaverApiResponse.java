package com.part3_team4.deokhoogam.domain.book.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
@JsonIgnoreProperties(ignoreUnknown = true)
public record NaverApiResponse(
    Integer total,
    List<NaverBookDto> items
) {}
