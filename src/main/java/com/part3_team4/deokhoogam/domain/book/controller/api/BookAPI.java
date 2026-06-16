package com.part3_team4.deokhoogam.domain.book.controller.api;

import com.part3_team4.deokhoogam.domain.book.dto.BookCreateRequest;
import com.part3_team4.deokhoogam.domain.book.dto.BookDto;
import com.part3_team4.deokhoogam.domain.book.dto.BookGetListRequest;
import com.part3_team4.deokhoogam.domain.book.dto.BookUpdateRequest;
import com.part3_team4.deokhoogam.domain.book.dto.NaverBookDto;
import com.part3_team4.deokhoogam.domain.book.dto.ranking.BookRankingDto;
import com.part3_team4.deokhoogam.domain.book.dto.ranking.RankingGetListRequest;
import com.part3_team4.deokhoogam.global.common.PageResponse;
import com.part3_team4.deokhoogam.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "도서 관리", description = "도서관리 API")
public interface BookAPI {

  @Operation(summary = "도서 등록", description = "새로운 도서를 등록합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "도서 등록 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 (입력값 검증 실패, ISBN 형식 오류 등)"),
      @ApiResponse(responseCode = "409", description = "ISBN 중복"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  ResponseEntity<BookDto> createBook(
      @Parameter(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
      @Schema(description = "도서 정보") @RequestPart("bookData") @Valid BookCreateRequest request,

      @RequestPart(value = "thumbnailImage", required = false)
      @Schema(description = "도서 썸네일 이미지") MultipartFile thumbnailImage
  );

  @Operation(summary = "도서 정보 수정", description = "도서 정보를 수정합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "도서 정보 수정 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 (입력값 검증 실패, ISBN 형식 오류 등)"),
      @ApiResponse(responseCode = "404", description = "도서 정보 없음"),
      @ApiResponse(responseCode = "409", description = "ISBN 중복"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  ResponseEntity<BookDto> updateBook(
      @Schema(description = "도서 ID", example = "123e4567-e89b-12d3-a456-426614174000") @PathVariable UUID bookId,

      @Parameter(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
      @Schema(description = "수정할 도서 정보") @RequestPart("bookData") @Valid BookUpdateRequest request,

      @RequestPart(value = "thumbnailImage", required = false)
      @Schema(description = "수정할 도서 썸네일 이미지") MultipartFile thumbnailImage
  );

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


  @Operation(summary = "ISBN으로 도서 정보 조회")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "도서 정보 조회 성공",
          content = @Content(schema = @Schema(implementation = NaverBookDto.class))
      ),
      @ApiResponse(responseCode = "400", description = "잘못된 ISBN형식",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "도서 정보 없음",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<NaverBookDto> getByISBN(@RequestParam("isbn") String isbn);


  @Operation(summary = "도서 목록 조회")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "도서 목록 조회 성공",
          content = @Content(schema = @Schema(implementation = PageResponse.class))),
      @ApiResponse(responseCode = "400", description = "잘못된 입력 값",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<PageResponse<BookDto>> getBooks(
      @Valid @ModelAttribute BookGetListRequest request
  );

  @Operation(summary = "인기도서 목록 조회")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "인기 도서 목록 조회 성공",
          content = @Content(schema = @Schema(implementation = PageResponse.class))),
      @ApiResponse(responseCode = "400", description = "잘못된 요청(랭킹 기간 오류, 정렬 방향 오류 등)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<PageResponse<BookRankingDto>> getRankings(
      @Valid @ModelAttribute RankingGetListRequest request);
}