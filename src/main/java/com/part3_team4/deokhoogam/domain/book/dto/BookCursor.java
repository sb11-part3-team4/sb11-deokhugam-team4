package com.part3_team4.deokhoogam.domain.book.dto;


import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BookCursor {
  private String mainValue;
  private UUID id;
  private String createdAt;
}
