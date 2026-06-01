package com.part3_team4.deokhoogam.domain.book.controller;

import com.part3_team4.deokhoogam.domain.book.dto.BookCreateRequest;
import com.part3_team4.deokhoogam.domain.book.dto.BookDto;
import com.part3_team4.deokhoogam.domain.book.dto.BookUpdateRequest;
import com.part3_team4.deokhoogam.domain.book.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "도서 관리", description = "도서 관련 API")
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

  private final BookService bookService;

  @Operation(summary = "도서 등록", description = "새로운 도서를 등록합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "도서 등록 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 (입력값 검증 실패, ISBN 형식 오류 등)"),
      @ApiResponse(responseCode = "409", description = "ISBN 중복"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<BookDto> createBook(
      @RequestPart("bookData") @Valid @Parameter(description = "도서 정보") BookCreateRequest request) {
    BookDto response = bookService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }


  @Operation(summary = "도서 정보 수정", description = "도서 정보를 수정합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "도서 정보 수정 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 (입력값 검증 실패, ISBN 형식 오류 등)"),
      @ApiResponse(responseCode = "404", description = "도서 정보 없음"),
      @ApiResponse(responseCode = "409", description = "ISBN 중복"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  @PatchMapping(value = "/{bookId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<BookDto> updateBook(
      @Parameter(description = "도서 ID", example = "123e4567-e89b-12d3-a456-426614174000") @PathVariable UUID bookId,
      @Parameter(description = "수정할 도서 정보") @RequestPart("bookData") @Valid BookUpdateRequest request
  ) {
    BookDto response = bookService.update(bookId, request);
    return ResponseEntity.ok(response);
  }
}