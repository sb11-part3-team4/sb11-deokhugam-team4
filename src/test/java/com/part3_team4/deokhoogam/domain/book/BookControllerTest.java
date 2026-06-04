package com.part3_team4.deokhoogam.domain.book;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.part3_team4.deokhoogam.domain.book.controller.BookController;
import com.part3_team4.deokhoogam.domain.book.dto.BookCreateRequest;
import com.part3_team4.deokhoogam.domain.book.dto.BookDto;
import com.part3_team4.deokhoogam.domain.book.dto.BookUpdateRequest;
import com.part3_team4.deokhoogam.domain.book.exception.BookNotFoundException;
import com.part3_team4.deokhoogam.domain.book.exception.IsbnAlreadyExistsException;
import com.part3_team4.deokhoogam.domain.book.service.BookService;
import com.part3_team4.deokhoogam.global.exception.ErrorCode;
import com.part3_team4.deokhoogam.global.fixture.BookFixtures;
import com.part3_team4.deokhoogam.global.jwt.JwtFilter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
  private JwtFilter jwtFilter;

  @Test
  @DisplayName("올바른 도서 정보로 생성 요청 시에 201 Created를 반환한다")
  void createBook_validRequest_returnsCreatedBook() throws Exception {
    // given
    BookCreateRequest request = BookFixtures.validBookCreateRequest();
    MockMultipartFile bookDataPart = createMockMultipartFile(request);

    UUID mockId = UUID.randomUUID();
    BookDto mockResponse = BookDto.builder()
        .id(mockId)
        .isbn(request.isbn())
        .title(request.title())
        .author(request.author())
        .build();

    given(bookService.create(any(BookCreateRequest.class))).willReturn(mockResponse);

    // when & then
    mockMvc.perform(multipart("/api/books")
            .file(bookDataPart))
        .andDo(print())
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(mockId.toString()))
        .andExpect(jsonPath("$.isbn").value(request.isbn()))
        .andExpect(jsonPath("$.title").value(request.title()));
  }


  @Test
  @DisplayName("이미 존재하는 ISBN으로 도서를 등록 시에 409 Conflict를 반환한다")
  void createBook_alreadyIsbn_returnsConflict() throws Exception {
    // given
    BookCreateRequest request = BookFixtures.validBookCreateRequest();
    MockMultipartFile bookDataPart = createMockMultipartFile(request);

    given(bookService.create(any(BookCreateRequest.class)))
        .willThrow(IsbnAlreadyExistsException.withIsbn(request.isbn()));

    // when & then
    mockMvc.perform(multipart("/api/books")
            .file(bookDataPart))
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

    MockMultipartFile bookDataPart = createMockMultipartFile(invalidRequest);

    // when & then
    mockMvc.perform(multipart("/api/books")
            .file(bookDataPart))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()))
        .andExpect(jsonPath("$.details.title").exists());
  }

  @Test
  @DisplayName("올바른 도서 정보로 수정 요청 시에 200 OK를 반환한다")
  void updateBook_validRequest_returnsOk() throws Exception {
    // given
    UUID targetId = UUID.randomUUID();
    BookUpdateRequest request = BookFixtures.validBookUpdateRequest();
    MockMultipartFile bookDataPart = createMockMultipartFile(request);

    BookDto mockResponse = BookDto.builder()
        .id(targetId)
        .title(request.title())
        .author(request.author())
        .description(request.description())
        .publisher(request.publisher())
        .publishedDate(request.publishedDate())
        .build();

    given(bookService.update(any(UUID.class), any(BookUpdateRequest.class))).willReturn(
        mockResponse);

    // when & then
    mockMvc.perform(multipart(HttpMethod.PATCH, "/api/books/{bookId}", targetId)
            .file(bookDataPart))
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

    MockMultipartFile bookDataPart = createMockMultipartFile(invalidRequest);

    // when & then
    mockMvc.perform(multipart(HttpMethod.PATCH, "/api/books/{bookId}", targetId)
            .file(bookDataPart))
        .andDo(print())
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("존재하지 않는 도서 ID로 수정 요청 시에 404 Not Found를 반환한다")
  void updateBook_nonExistentId_returnsNotFound() throws Exception {
    // given
    UUID nonExistentId = UUID.randomUUID();
    BookUpdateRequest request = BookFixtures.validBookUpdateRequest();
    MockMultipartFile bookDataPart = createMockMultipartFile(request);

    given(bookService.update(any(UUID.class), any(BookUpdateRequest.class)))
        .willThrow(BookNotFoundException.withId(nonExistentId));

    // when & then
    mockMvc.perform(multipart(HttpMethod.PATCH, "/api/books/{bookId}", nonExistentId)
            .file(bookDataPart))
        .andDo(print())
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("도서 수정 중 ISBN 중복 예외 발생 시에 409 Conflict를 반환한다")
  void updateBook_isbnConflict_returnsConflict() throws Exception {
    // given
    UUID targetId = UUID.randomUUID();
    BookUpdateRequest request = BookFixtures.validBookUpdateRequest();
    MockMultipartFile bookDataPart = createMockMultipartFile(request);

    given(bookService.update(any(UUID.class), any(BookUpdateRequest.class)))
        .willThrow(IsbnAlreadyExistsException.withIsbn("1234567890123"));

    // when & then
    mockMvc.perform(multipart(HttpMethod.PATCH, "/api/books/{bookId}", targetId)
            .file(bookDataPart))
        .andDo(print())
        .andExpect(status().isConflict());
  }

  // MultipartFile 생성 헬퍼 메서드
  private MockMultipartFile createMockMultipartFile(Object request) throws Exception {
    return new MockMultipartFile(
        "bookData",
        "",
        MediaType.APPLICATION_JSON_VALUE,
        objectMapper.writeValueAsBytes(request)
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