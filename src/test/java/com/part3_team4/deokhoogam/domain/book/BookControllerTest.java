package com.part3_team4.deokhoogam.domain.book;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import com.part3_team4.deokhoogam.global.fixture.BookFixtures;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
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
}