package com.part3_team4.deokhoogam.domain.book.controller;

import com.part3_team4.deokhoogam.domain.book.controller.api.BookAPI;
import com.part3_team4.deokhoogam.domain.book.dto.BookCreateRequest;
import com.part3_team4.deokhoogam.domain.book.dto.BookDto;
import com.part3_team4.deokhoogam.domain.book.dto.BookGetListRequest;
import com.part3_team4.deokhoogam.domain.book.dto.BookUpdateRequest;
import com.part3_team4.deokhoogam.domain.book.dto.NaverBookDto;
import com.part3_team4.deokhoogam.domain.book.service.BookService;
import com.part3_team4.deokhoogam.domain.book.service.OcrService;
import com.part3_team4.deokhoogam.global.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "도서 관리", description = "도서 관련 API")
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController implements BookAPI {

  private final BookService bookService;
  private final OcrService ocrService;

  @Operation(summary = "도서 등록", description = "새로운 도서를 등록합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "도서 등록 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 (입력값 검증 실패, ISBN 형식 오류 등)"),
      @ApiResponse(responseCode = "409", description = "ISBN 중복"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<BookDto> createBook(
      @Parameter(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
      @Schema(description = "도서 정보") @RequestPart("bookData") @Valid BookCreateRequest request,

      @RequestPart(value = "thumbnailImage", required = false)
      @Schema(description = "도서 썸네일 이미지") MultipartFile thumbnailImage
  ) {
    BookDto response = bookService.create(request, thumbnailImage);
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
      @Schema(description = "도서 ID", example = "123e4567-e89b-12d3-a456-426614174000") @PathVariable UUID bookId,

      @Parameter(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
      @Schema(description = "수정할 도서 정보") @RequestPart("bookData") @Valid BookUpdateRequest request,

      @RequestPart(value = "thumbnailImage", required = false)
      @Schema(description = "수정할 도서 썸네일 이미지") MultipartFile thumbnailImage
  ) {
    BookDto response = bookService.update(bookId, request, thumbnailImage);
    return ResponseEntity.ok(response);
  }

  @Override
  @GetMapping(value = "/{bookId}")
  public ResponseEntity<BookDto> getDetails(@PathVariable UUID bookId) {

    BookDto response = bookService.getDetails(bookId);

    return ResponseEntity.ok().body(response);

  }

  @Override
  @GetMapping("/info")
  public ResponseEntity<NaverBookDto> getByISBN(String isbn) {
    NaverBookDto response = bookService.getByIsbn(isbn);
    return ResponseEntity.ok().body(response);
  }


  @Override
  @GetMapping
  public ResponseEntity<PageResponse<BookDto>> getBooks(BookGetListRequest request) {
    PageResponse<BookDto> response = bookService.getBooks(request);

    return ResponseEntity.ok().body(response);

  }

  // TODO: 임시 Mock 응답 - 실제 인기 도서 페이지네이션 로직 구현 필요
  @GetMapping("/popular")
  public ResponseEntity<Map<String, Object>> getPopularBooks() {
    return ResponseEntity.ok(Map.of(
        "content", Collections.emptyList(),
        "hasNext", false,
        "totalElements", 0,
        "size", 0
    ));
  }

  @Operation(summary = "OCR 기반 ISBN 인식", description = "OCR을 통해 ISBN을 인식합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "ISBN 인식 성공"),
      @ApiResponse(responseCode = "400", description = "이미지 형식 오류 또는 OCR 인식 실패"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  @PostMapping(value = "/isbn/ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<String> extractIsbn(
      @RequestParam("image") @Schema(description = "도서 이미지") MultipartFile image) {
    String extractedIsbn = ocrService.extractIsbnFromImage(image);

    return ResponseEntity.ok(extractedIsbn);
  }
}