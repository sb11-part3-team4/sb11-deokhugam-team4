package com.part3_team4.deokhoogam.domain.book.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookUpdateRequest {

  @NotBlank(message = "제목은 필수입니다")
  @Length(max = 255, message = "제목은 255자 이하여야 합니다")
  private String title;

  @NotBlank(message = "저자는 필수입니다")
  @Length(max = 100, message = "저자는 100자 이하여야 합니다")
  private String author;

  @NotBlank(message = "설명은 필수입니다")
  private String description;

  @NotBlank(message = "출판사는 필수입니다")
  @Length(max = 100, message = "출판사는 100자 이하여야 합니다")
  private String publisher;

  @NotNull(message = "출판일은 필수입니다")
  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate publishedDate;
}