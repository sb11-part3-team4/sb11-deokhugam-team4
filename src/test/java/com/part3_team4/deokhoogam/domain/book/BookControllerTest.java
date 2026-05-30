package com.part3_team4.deokhoogam.domain.book;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
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
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BookController.class)
@ActiveProfiles("test")
@DisplayName("BookController 단위 테스트")
class BookControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private BookService bookService;


  @Test
  @DisplayName("올바른 도서 정보로 생성 요청 시에 201 Created를 반환한다")
  void createBook_validRequest_returnsCreatedBook() throws Exception {
    // given
    BookCreateRequest request = createValidBookRequest();
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
    BookCreateRequest request = createValidBookRequest();
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
    BookCreateRequest invalidRequest = BookCreateRequest.builder()
        .isbn("1234567890123")
        .title("")
        .author("조슈아 블로흐")
        .description("자바 가이드")
        .publisher("인사이트")
        .publishedDate(LocalDate.of(2026, 5, 28))
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
  @DisplayName("ISBN에 숫자가 아닌 값이 섞여있으면 400 Bad Request를 반환한다")
  void createBook_isbnContainsLetters_returnsBadRequest() throws Exception {
    // given
    BookCreateRequest invalidRequest = BookCreateRequest.builder()
        .isbn("1234567890abc")
        .title("이펙티브 자바")
        .author("조슈아 블로흐")
        .description("자바 가이드")
        .publisher("인사이트")
        .publishedDate(LocalDate.of(2026, 5, 28))
        .build();

    MockMultipartFile bookDataPart = createMockMultipartFile(invalidRequest);

    // when & then
    mockMvc.perform(multipart("/api/books")
            .file(bookDataPart))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()))
        .andExpect(jsonPath("$.details.isbn").exists());
  }

  @Test
  @DisplayName("ISBN이 20자를 초과하는 경우 400 Bad Request를 반환한다")
  void createBook_isbnTooLong_returnsBadRequest() throws Exception {
    // given
    BookCreateRequest invalidRequest = BookCreateRequest.builder()
        .isbn("123456789012345678901")
        .title("이펙티브 자바")
        .author("조슈아 블로흐")
        .description("자바 가이드")
        .publisher("인사이트")
        .publishedDate(LocalDate.of(2026, 5, 28))
        .build();

    MockMultipartFile bookDataPart = createMockMultipartFile(invalidRequest);

    // when & then
    mockMvc.perform(multipart("/api/books")
            .file(bookDataPart))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()))
        .andExpect(jsonPath("$.details.isbn").exists());
  }

  // 등록 픽스처 메소드
  private BookCreateRequest createValidBookRequest() {
    return BookCreateRequest.builder()
        .isbn("1234567890123")
        .title("이펙티브 자바")
        .author("조슈아 블로흐")
        .description("자바 가이드")
        .publisher("인사이트")
        .publishedDate(LocalDate.of(2026, 5, 28))
        .build();
  }

  @Test
  @DisplayName("올바른 도서 정보로 수정 요청 시에 200 OK를 반환한다")
  void updateBook_validRequest_returnsOk() throws Exception {
    // given
    UUID targetId = UUID.randomUUID();
    BookUpdateRequest request = createValidBookUpdateRequest();
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
    BookUpdateRequest invalidRequest = BookUpdateRequest.builder()
        .title("")
        .author("로버트 C. 마틴")
        .description("소프트웨어 구조와 설계의 원칙")
        .publisher("인사이트")
        .publishedDate(LocalDate.of(2026, 5, 29))
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
    BookUpdateRequest request = createValidBookUpdateRequest();
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
    BookUpdateRequest request = createValidBookUpdateRequest();
    MockMultipartFile bookDataPart = createMockMultipartFile(request);

    given(bookService.update(any(UUID.class), any(BookUpdateRequest.class)))
        .willThrow(IsbnAlreadyExistsException.withIsbn("1234567890123"));

    // when & then
    mockMvc.perform(multipart(HttpMethod.PATCH, "/api/books/{bookId}", targetId)
            .file(bookDataPart))
        .andDo(print())
        .andExpect(status().isConflict());
  }

  // 수정 픽스처 메서드
  private BookUpdateRequest createValidBookUpdateRequest() {
    return BookUpdateRequest.builder()
        .title("클린 아키텍처")
        .author("로버트 C. 마틴")
        .description("소프트웨어 구조와 설계의 원칙")
        .publisher("인사이트")
        .publishedDate(LocalDate.of(2026, 5, 29))
        .build();
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
}