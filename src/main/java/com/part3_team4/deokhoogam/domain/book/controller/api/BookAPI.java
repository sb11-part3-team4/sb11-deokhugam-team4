package com.part3_team4.deokhoogam.domain.book.controller.api;

import com.part3_team4.deokhoogam.domain.book.dto.BookDto;
import com.part3_team4.deokhoogam.domain.book.dto.BookGetListRequest;
import com.part3_team4.deokhoogam.global.common.PageResponse;
import com.part3_team4.deokhoogam.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
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


  @Operation(summary = "도서 목록 조회")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "도서 목록 조회 성공",
      content = @Content(schema = @Schema(implementation = PageResponse.class))),
      @ApiResponse(responseCode = "400", description = "잘못된 입력값",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<PageResponse<BookDto>> getBooks(
      @Valid @ModelAttribute BookGetListRequest request
  );




}
