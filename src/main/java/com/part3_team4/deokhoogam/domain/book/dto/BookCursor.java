package com.part3_team4.deokhoogam.domain.book.dto;


import com.part3_team4.deokhoogam.domain.book.entity.SortType;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BookCursor {
  private SortType mainValue;
  private UUID id;
  private Instant createdAt;
}
