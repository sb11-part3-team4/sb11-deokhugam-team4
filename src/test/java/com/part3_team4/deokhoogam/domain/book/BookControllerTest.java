package com.part3_team4.deokhoogam.domain.book;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.part3_team4.deokhoogam.domain.book.Factory.BookFixtureFactory;
import com.part3_team4.deokhoogam.domain.book.controller.BookController;
import com.part3_team4.deokhoogam.domain.book.dto.BookCreateRequest;
import com.part3_team4.deokhoogam.domain.book.dto.BookDto;
import com.part3_team4.deokhoogam.domain.book.exception.BookNotFoundException;
import com.part3_team4.deokhoogam.domain.book.exception.IsbnAlreadyExistsException;
import com.part3_team4.deokhoogam.domain.book.service.BookService;
import com.part3_team4.deokhoogam.global.common.PageResponse;
import com.part3_team4.deokhoogam.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

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

    MockMultipartFile bookDataPart = new MockMultipartFile(
        "bookData",
        "",
        MediaType.APPLICATION_JSON_VALUE,
        objectMapper.writeValueAsBytes(request)
    );

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

    MockMultipartFile bookDataPart = new MockMultipartFile(
        "bookData",
        "",
        MediaType.APPLICATION_JSON_VALUE,
        objectMapper.writeValueAsBytes(request)
    );

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

    MockMultipartFile bookDataPart = new MockMultipartFile(
        "bookData",
        "",
        MediaType.APPLICATION_JSON_VALUE,
        objectMapper.writeValueAsBytes(invalidRequest)
    );

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

    MockMultipartFile bookDataPart = new MockMultipartFile(
        "bookData", "", MediaType.APPLICATION_JSON_VALUE,
        objectMapper.writeValueAsBytes(invalidRequest)
    );

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

    MockMultipartFile bookDataPart = new MockMultipartFile(
        "bookData", "", MediaType.APPLICATION_JSON_VALUE,
        objectMapper.writeValueAsBytes(invalidRequest)
    );

    // when & then
    mockMvc.perform(multipart("/api/books")
            .file(bookDataPart))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()))
        .andExpect(jsonPath("$.details.isbn").exists());
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
            .andExpect(jsonPath("$.details.limit").exists());;


      }


    }


  }


  // 픽스처 메소드
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