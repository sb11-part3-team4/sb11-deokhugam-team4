package com.part3_team4.deokhoogam.domain.book.infrastructure.ocr.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;

@Getter
public class OcrSpaceDto {

  @JsonProperty("IsErroredOnProcessing")
  private boolean isErroredOnProcessing;

  @JsonProperty("ErrorMessage")
  private List<String> errorMessage;

  @JsonProperty("ParsedResults")
  private List<ParsedResult> parsedResults;

  @Getter
  public static class ParsedResult {

    @JsonProperty("ParsedText")
    private String parsedText;
  }
}