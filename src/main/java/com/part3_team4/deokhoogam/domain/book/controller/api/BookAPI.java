package com.part3_team4.deokhoogam.domain.book.controller.api;

import com.part3_team4.deokhoogam.domain.book.dto.BookDto;
import com.part3_team4.deokhoogam.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "도서 관리", description = "도서관리 API")
public interface BookAPI {

  @Operation(summary = "도서 상세 정보 조회")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "도서 정보 조회 성공",
      content = @Content(schema = @Schema(implementation = BookDto.class))
    ),
      @ApiResponse(responseCode = "404", description = "도서 정보 없음",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<BookDto> getDetails(
      @PathVariable UUID bookId
  );

  @Operation(summary = "도서 상세 논리 삭제")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "도서 삭제 성공"),
      @ApiResponse(responseCode = "404", description = "도서 정보 없음")
  })
  ResponseEntity<Void> deleteBook(
      @PathVariable UUID bookId
  );

  @Operation(summary = "도서 상세 물리 삭제")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "도서 삭제 성공"),
      @ApiResponse(responseCode = "404", description = "도서 정보 없음")
  })
  ResponseEntity<Void> deleteBookHard(
      @PathVariable UUID bookId
  );







}
