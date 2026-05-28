package com.part3_team4.deokhoogam.domain.book.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.part3_team4.deokhoogam.domain.book.entity.Book;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Builder;
import org.hibernate.validator.constraints.Length;

@Builder
public record BookCreateRequest(

    @NotBlank(message = "제목은 필수입니다")
    @Length(max = 255, message = "제목은 255자 이하여야 합니다")
    String title,

    @NotBlank(message = "저자는 필수입니다")
    @Length(max = 100, message = "저자는 100자 이하여야 합니다")
    String author,

    @NotBlank(message = "설명은 필수입니다")
    String description,

    @NotBlank(message = "출판사는 필수입니다")
    @Length(max = 100, message = "출판사는 100자 이하여야 합니다")
    String publisher,

    @NotNull(message = "출판일은 필수입니다")
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate publishedDate,

    String isbn
) {

  public Book toEntity() {
    return Book.builder()
        .isbn(this.isbn)
        .title(this.title)
        .author(this.author)
        .description(this.description)
        .publisher(this.publisher)
        .publishedDate(this.publishedDate)
        .build();
  }
}