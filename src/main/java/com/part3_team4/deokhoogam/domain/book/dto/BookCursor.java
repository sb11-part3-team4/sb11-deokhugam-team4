package com.part3_team4.deokhoogam.domain.book.dto;


import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookCursor {

  private String mainValue;
  private UUID id;
  private Instant createdAt;
}
