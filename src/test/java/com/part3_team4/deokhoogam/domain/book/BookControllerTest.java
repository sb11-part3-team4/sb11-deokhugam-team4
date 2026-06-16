package com.part3_team4.deokhoogam.domain.book;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.part3_team4.deokhoogam.domain.book.controller.BookController;
import com.part3_team4.deokhoogam.domain.book.dto.BookCreateRequest;
import com.part3_team4.deokhoogam.domain.book.dto.BookDto;
import com.part3_team4.deokhoogam.domain.book.dto.BookUpdateRequest;
import com.part3_team4.deokhoogam.domain.book.dto.NaverBookDto;
import com.part3_team4.deokhoogam.domain.book.exception.BookNotFoundException;
import com.part3_team4.deokhoogam.domain.book.exception.InvalidIsbnException;
import com.part3_team4.deokhoogam.domain.book.exception.IsbnAlreadyExistsException;
import com.part3_team4.deokhoogam.domain.book.exception.OcrProcessingException;
import com.part3_team4.deokhoogam.domain.book.service.BookRankingService;
import com.part3_team4.deokhoogam.domain.book.service.BookService;
import com.part3_team4.deokhoogam.domain.book.service.OcrService;
import com.part3_team4.deokhoogam.global.common.PageResponse;
import com.part3_team4.deokhoogam.global.exception.ErrorCode;
import com.part3_team4.deokhoogam.global.fixture.BookFixtureFactory;
import com.part3_team4.deokhoogam.global.fixture.BookFixtures;
import com.part3_team4.deokhoogam.global.fixture.NaverBookFixture;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;


@WebMvcTest(BookController.class)
@ActiveProfiles("test")
@DisplayName("BookController 단위 테스트")
@AutoConfigureMockMvc(addFilters = false)
class BookControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private BookService bookService;

  @MockitoBean
  private OcrService ocrService;

  @MockitoBean
  private BookRankingService bookRankingService;

  @Nested
  @DisplayName("도서 등록 API에서")
  class TestCreateBook {

    @Test
    @DisplayName("올바른 도서 정보로 생성 요청 시에 201 Created를 반환한다")
    void createBook_validRequest_returnsCreatedBook() throws Exception {
      // given
      BookCreateRequest request = BookFixtures.validBookCreateRequest();
      UUID mockId = UUID.randomUUID();

      BookDto mockResponse = BookDto.builder()
          .id(mockId)
          .isbn(request.isbn())
          .title(request.title())
          .author(request.author())
          .build();

      given(bookService.create(any(BookCreateRequest.class), isNull()))
          .willReturn(mockResponse);

      // when & then
      mockMvc.perform(createBookRequest(request))
          .andDo(print())
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id").value(mockId.toString()))
          .andExpect(jsonPath("$.isbn").value(request.isbn()))
          .andExpect(jsonPath("$.title").value(request.title()));
    }

    @Test
    @DisplayName("썸네일 파일과 함께 도서 생성 요청 시 201 Created를 반환한다")
    void createBook_withThumbnail_returnsCreatedBook() throws Exception {
      // given
      BookCreateRequest request = BookFixtures.validBookCreateRequest();

      UUID mockId = UUID.randomUUID();

      BookDto mockResponse = BookDto.builder()
          .id(mockId)
          .title(request.title())
          .isbn(request.isbn())
          .author(request.author())
          .build();

      given(bookService.create(any(BookCreateRequest.class), any()))
          .willReturn(mockResponse);

      // when & then
      mockMvc.perform(createBookRequest(request, createThumbnailFile()))
          .andDo(print())
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id").value(mockId.toString()))
          .andExpect(jsonPath("$.title").value(request.title()));
    }

    @Test
    @DisplayName("이미 존재하는 ISBN으로 도서를 등록 시에 409 Conflict를 반환한다")
    void createBook_alreadyIsbn_returnsConflict() throws Exception {
      // given
      BookCreateRequest request = BookFixtures.validBookCreateRequest();

      given(bookService.create(any(BookCreateRequest.class), isNull()))
          .willThrow(IsbnAlreadyExistsException.withIsbn(request.isbn()));

      // when & then
      mockMvc.perform(createBookRequest(request))
          .andDo(print())
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value(ErrorCode.ISBN_ALREADY_EXISTS.getCode()))
          .andExpect(jsonPath("$.message").value(ErrorCode.ISBN_ALREADY_EXISTS.getMessage()));
    }

    @Test
    @DisplayName("유효하지 않은 정보로 생성 요청 시에 400 Bad Request를 반환한다")
    void createBook_invalidRequest_returnsBadRequest() throws Exception {
      // given
      BookCreateRequest invalidRequest = BookFixtures.validBookCreateRequest().toBuilder()
          .title("")
          .build();

      // when & then
      mockMvc.perform(createBookRequest(invalidRequest))
          .andDo(print())
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()))
          .andExpect(jsonPath("$.details.title").exists());
    }
  }

  @Nested
  @DisplayName("도서 수정 API에서")
  class TestUpdateBook {

    @Test
    @DisplayName("올바른 도서 정보로 수정 요청 시에 200 OK를 반환한다")
    void updateBook_validRequest_returnsOk() throws Exception {
      // given
      UUID targetId = UUID.randomUUID();
      BookUpdateRequest request = BookFixtures.validBookUpdateRequest();

      BookDto mockResponse = BookDto.builder()
          .id(targetId)
          .title(request.title())
          .author(request.author())
          .description(request.description())
          .publisher(request.publisher())
          .publishedDate(request.publishedDate())
          .build();

      given(bookService.update(any(UUID.class), any(BookUpdateRequest.class), isNull()))
          .willReturn(mockResponse);

      // when & then
      mockMvc.perform(updateBookRequest(targetId, request))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.title").value(request.title()));
    }

    @Test
    @DisplayName("썸네일 파일과 함께 도서 수정 요청 시 200 OK를 반환한다")
    void updateBook_withThumbnail_returnsOk() throws Exception {
      // given
      UUID targetId = UUID.randomUUID();
      BookUpdateRequest request = BookFixtures.validBookUpdateRequest();
      MockMultipartFile thumbnail = createThumbnailFile();

      BookDto mockResponse = BookDto.builder()
          .id(targetId)
          .title(request.title())
          .build();

      given(bookService.update(eq(targetId), any(BookUpdateRequest.class), any()))
          .willReturn(mockResponse);

      // when & then
      mockMvc.perform(updateBookRequest(targetId, request, thumbnail))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.title").value(request.title()));
    }

    @Test
    @DisplayName("유효하지 않은 정보로 수정 요청 시에 400 Bad Request를 반환한다")
    void updateBook_invalidRequest_returnsBadRequest() throws Exception {
      // given
      UUID targetId = UUID.randomUUID();
      BookUpdateRequest invalidRequest = BookFixtures.validBookUpdateRequest().toBuilder()
          .title("")
          .build();

      // when & then
      mockMvc.perform(updateBookRequest(targetId, invalidRequest))
          .andDo(print())
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("존재하지 않는 도서 ID로 수정 요청 시에 404 Not Found를 반환한다")
    void updateBook_nonExistentId_returnsNotFound() throws Exception {
      // given
      UUID nonExistentId = UUID.randomUUID();
      BookUpdateRequest request = BookFixtures.validBookUpdateRequest();

      // 수정 후
      given(bookService.update(any(UUID.class), any(BookUpdateRequest.class), isNull()))
          .willThrow(BookNotFoundException.withId(nonExistentId));

      // when & then
      mockMvc.perform(updateBookRequest(nonExistentId, request))
          .andDo(print())
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("도서 수정 중 ISBN 중복 예외 발생 시에 409 Conflict를 반환한다")
    void updateBook_isbnConflict_returnsConflict() throws Exception {
      // given
      UUID targetId = UUID.randomUUID();

      BookUpdateRequest request = BookFixtures.validBookUpdateRequest();

      given(bookService.update(any(UUID.class), any(BookUpdateRequest.class), isNull()))
          .willThrow(IsbnAlreadyExistsException.withIsbn("1234567890123"));

      // when & then
      mockMvc.perform(updateBookRequest(targetId, request))
          .andDo(print())
          .andExpect(status().isConflict());
    }
  }

  @Nested
  @DisplayName("도서 OCR 인식 API에서")
  class TestOCRBook {

    @Test
    @DisplayName("이미지를 업로드하면 ISBN을 문자열로 반환한다")
    void extractIsbn_validImage_returnsOk() throws Exception {
      // given
      MockMultipartFile image = createOcrImageFile();

      given(ocrService.extractIsbnFromImage(any())).willReturn("9788965402602");

      // when & then
      mockMvc.perform(extractIsbnRequest(image))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(content().string("9788965402602"));
    }

    @Test
    @DisplayName("OCR 인식 실패 시 400 Bad Request를 반환한다")
    void extractIsbn_ocrFails_returnsBadRequest() throws Exception {
      // given
      MockMultipartFile image = createOcrImageFile();

      given(ocrService.extractIsbnFromImage(any()))
          .willThrow(OcrProcessingException.from(ErrorCode.OCR_PROCESSING_FAILED));

      // when & then
      mockMvc.perform(extractIsbnRequest(image))
          .andDo(print())
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value(ErrorCode.OCR_PROCESSING_FAILED.getCode()));
    }
  }

  // ============ 헬퍼 메서드 =============

  // bookData만 요청
  private MockMultipartHttpServletRequestBuilder createBookRequest(BookCreateRequest request
  ) throws Exception {
    return multipart("/api/books")
        .file(createJsonPart(request));
  }

  // bookData와 thumbnailImage 요청
  private MockMultipartHttpServletRequestBuilder createBookRequest(BookCreateRequest request,
      MockMultipartFile thumbnail
  ) throws Exception {
    return multipart("/api/books")
        .file(createJsonPart(request))
        .file(thumbnail);
  }

  private MockMultipartHttpServletRequestBuilder updateBookRequest(UUID bookId,
      BookUpdateRequest request
  ) throws Exception {
    return multipart(HttpMethod.PATCH, "/api/books/{bookId}", bookId)
        .file(createJsonPart(request));
  }

  private MockMultipartFile createJsonPart(Object request) throws Exception {
    return new MockMultipartFile(
        "bookData",
        "",
        MediaType.APPLICATION_JSON_VALUE,
        objectMapper.writeValueAsBytes(request)
    );
  }

  private MockMultipartFile createThumbnailFile() {
    return new MockMultipartFile(
        "thumbnailImage",
        "thumbnail.jpg",
        MediaType.IMAGE_JPEG_VALUE,
        "dummy-image".getBytes()
    );
  }

  private MockMultipartHttpServletRequestBuilder updateBookRequest(UUID bookId,
      BookUpdateRequest request, MockMultipartFile thumbnail
  ) throws Exception {
    return multipart(HttpMethod.PATCH, "/api/books/{bookId}", bookId)
        .file(createJsonPart(request))
        .file(thumbnail);
  }

  private MockMultipartHttpServletRequestBuilder extractIsbnRequest(MockMultipartFile image) {
    return multipart("/api/books/isbn/ocr")
        .file(image);
  }

  private MockMultipartFile createOcrImageFile() {
    return new MockMultipartFile(
        "image",
        "book.jpg",
        MediaType.IMAGE_JPEG_VALUE,
        "dummy-image".getBytes()
    );
  }

  @Nested
  @DisplayName("getBookDetails() API에서")
  class TestGetBookDetails {

    @Test
    @DisplayName("유효한 ID로 요청이 들어오면 200 상태 코드로 책 상세 정보를 반환한다.")
    void return_200_when_valid_id() throws Exception {

      //given
      UUID mockId = UUID.randomUUID();

      BookDto bookDto = createValidBookDto(mockId);

      given(bookService.getDetails(mockId)).willReturn(bookDto);

      //when
      ResultActions result = mockMvc.perform(get("/api/books/{bookId}", mockId)
          .accept(MediaType.APPLICATION_JSON));

      //then
      result.andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(mockId.toString()))
          .andExpect(jsonPath("$.isbn").value(bookDto.isbn()));

    }

    @Test
    @DisplayName("유효하지 않은 ID가 들어오면 404 상태 코드를 반환한다")
    void return_404_when_invalid_id() throws Exception {

      //given
      UUID mockId = UUID.randomUUID();
      given(bookService.getDetails(mockId)).willThrow(BookNotFoundException.withId(mockId));

      //when
      ResultActions result = mockMvc.perform(get("/api/books/{bookId}", mockId)
          .accept(MediaType.APPLICATION_JSON));

      //then
      result.andExpect(status().isNotFound());

    }


    //픽스쳐
    private BookDto createValidBookDto(UUID mockId) {
      Instant at = Instant.now().minusSeconds(10);

      return BookDto.builder()
          .id(mockId)
          .title("모비 딕")
          .author("허먼 멜빌")
          .description("『모비 딕』 완역본")
          .publisher("작가정신")
          .publishedDate(LocalDate.of(2024, 4, 9))
          .thumbnailUrl("temp/url")
          .reviewCount(2)
          .rating(BigDecimal.valueOf(4.5))
          .isbn("9791160263404")
          .createdAt(at)
          .updatedAt(at)
          .build();
    }


  }

  @Nested
  @DisplayName("isbn으로 도서 정보 조회 api 에서")
  class TestGetBookByIsbn {

    String isbn = "9791060263004";

    @Test
    @DisplayName("유효한 isbn이 들어오면 도서 정보와 200을 리턴한다")
    void return_200_and_data_when_valid_isbn() throws Exception {

      //given

      //예시 응답
      NaverBookDto bookSimpleDto = NaverBookFixture.createValidNaverBookDto(isbn);
      given(bookService.getByIsbn(isbn)).willReturn(bookSimpleDto);

      //when
      ResultActions result = mockMvc.perform(get("/api/books/info")
          .param("isbn", isbn)
          .accept(MediaType.APPLICATION_JSON));

      result.andExpect(status().isOk());
      result.andExpect(jsonPath("$.isbn").value(isbn));


    }

    @Test
    @DisplayName("유효하지 않은 형태의 문자열이 들어오면 400 + 예외를 발생시킨다.")
    void return_400_when_invalid_isbn() throws Exception {

      //given

      String invalidIsbn = "97910adsf6026!!300";

      given(bookService.getByIsbn(invalidIsbn)).willThrow(
          InvalidIsbnException.withIsbn(invalidIsbn));

      ResultActions result = mockMvc.perform(get("/api/books/info")
          .param("isbn", invalidIsbn));

      result.andExpect(status().isBadRequest());


    }

    @Test
    @DisplayName("존재하지 않는 ISBN이 들어오면 404 + 예외를 발생시킨다.")
    void return_404_when_not_found_isbn() throws Exception {

      //given
      given(bookService.getByIsbn(isbn)).willThrow(BookNotFoundException.withIsbn(isbn));

      ResultActions result = mockMvc.perform(get("/api/books/info")
          .param("isbn", isbn));

      result.andExpect(status().isNotFound());


    }


  }

  @Nested
  @DisplayName("도서 목록 조회 에서")
  class TestGetBooks {

    @Nested
    @DisplayName("유효한 데이터가 들어오는 경우")
    class TestGetBooks_ValidData {

      @Test
      @DisplayName("최소한의 데이터가 들어올때 200을 리턴하고 디폴트 옵션으로 도서 목록을 리턴한다")
      void return_200_when_minimum_valid_data() throws Exception {
        //given
        List<BookDto> books = BookFixtureFactory.createBookDtoList();

        PageResponse<BookDto> response = new PageResponse<>(

            books,
            "Cursor",
            "After",
            50,
            4L,
            false
        );

        given(bookService.getBooks(any())).willReturn(response);

        //when
        ResultActions result = mockMvc.perform(get("/api/books")
            .accept(MediaType.APPLICATION_JSON));

        //then

        result.andExpect(status().isOk());
        result.andExpect(jsonPath("$.content[0].id").exists());
        result.andExpect(jsonPath("$.content[0].id").value(books.get(0).id().toString()));
        result.andExpect(jsonPath("$.content[0].title").exists());
        result.andExpect(jsonPath("$.content[0].title").value("모비 딕"));
        result.andExpect(jsonPath("$.content[0].isbn").exists());
        result.andExpect(jsonPath("$.content[0].isbn").value("9791160263404"));
        result.andExpect(jsonPath("$.content[1].id").exists());
        result.andExpect(jsonPath("$.content[2].id").exists());
        result.andExpect(jsonPath("$.content[3].id").exists());


      }


    }

    @Nested
    @DisplayName("유효하지 않은 데이터가 들어온 경우")
    class TestGetBooks_InvalidData {

      @Test
      @DisplayName("잘못된 입력 데이터가 들어왔을때 400 에러 코드를 반환한다")
      void return_400_when_invalid_data() throws Exception {

        //given & when
        ResultActions result = mockMvc.perform(get("/api/books")
            .param("limit", "-10") //페이지 수 음수
            .accept(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()))
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.details.limit").exists());


      }


    }


  }

  @Nested
  @DisplayName("도서 논리 삭제 API에서")
  class TestDeleteBook {

    UUID mockId = UUID.randomUUID();

    @Test
    @DisplayName("도서를 삭제하는데 성공하면 204를 반환한다")
    void return_204_when_delete_book() throws Exception {

      mockMvc.perform(delete("/api/books/{bookId}", mockId))
          .andExpect(status().isNoContent());

      verify(bookService).delete(mockId);

    }

    @Test
    @DisplayName("없는 도서 아이디를 입력받으면 404 에러코드를 반환한다")
    void return_404_when_delete_book_with_invalid_id() throws Exception {

      willThrow(BookNotFoundException.withId(mockId))
          .given(bookService).delete(any());

      mockMvc.perform(delete("/api/books/{bookId}", mockId))
          .andExpect(status().isNotFound());

    }


  }


  @Nested
  @DisplayName("도서 물리 삭제 API에서")
  class TestDeleteHardBook {

    UUID mockId = UUID.randomUUID();

    @Test
    @DisplayName("도서를 삭제하는데 성공하면 204를 반환한다")
    void return_204_when_delete_book_hard() throws Exception {

      mockMvc.perform(delete("/api/books/{bookId}/hard", mockId))
          .andExpect(status().isNoContent());

      verify(bookService).deleteHard(mockId);

    }

    @Test
    @DisplayName("없는 도서 아이디를 입력받으면 404 에러코드를 반환한다")
    void return_404_when_delete_book_hard_with_invalid_id() throws Exception {

      willThrow(BookNotFoundException.withId(mockId))
          .given(bookService).deleteHard(any());

      mockMvc.perform(delete("/api/books/{bookId}/hard", mockId))
          .andExpect(status().isNotFound());

    }


  }


}