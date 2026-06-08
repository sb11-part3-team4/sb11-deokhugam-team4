package com.part3_team4.deokhoogam.domain.book.infrastructure.ocr.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record OcrSpaceDto(
    @JsonProperty("IsErroredOnProcessing")
    boolean isErroredOnProcessing,

    @JsonProperty("ErrorMessage")
    List<String> errorMessage,

    @JsonProperty("ParsedResults")
    List<ParsedResult> parsedResults
) {

  public record ParsedResult(
      @JsonProperty("ParsedText")
      String parsedText
  ) {

  }
}