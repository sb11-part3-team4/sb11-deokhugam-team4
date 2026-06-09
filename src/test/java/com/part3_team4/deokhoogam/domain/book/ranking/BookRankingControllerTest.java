package com.part3_team4.deokhoogam.domain.book.ranking;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.part3_team4.deokhoogam.domain.book.controller.BookController;
import com.part3_team4.deokhoogam.domain.book.dto.ranking.BookRankingDto;
import com.part3_team4.deokhoogam.domain.book.entity.PeriodType;
import com.part3_team4.deokhoogam.domain.book.service.BookRankingService;
import com.part3_team4.deokhoogam.domain.book.service.BookService;
import com.part3_team4.deokhoogam.domain.book.service.OcrService;
import com.part3_team4.deokhoogam.global.common.PageResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


@WebMvcTest(BookController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")

class BookRankingControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private BookRankingService bookRankingService;

  @MockitoBean
  private BookService bookService;

  @MockitoBean
  private OcrService ocrService;


  @Test
  @DisplayName("정상 요청이면 200과 랭킹 목록을 반환한다")
  void return_200_and_data_when_valid_request() throws Exception {
    //given

    BookRankingDto dto = new BookRankingDto(
        UUID.randomUUID(), UUID.randomUUID(), "모비 딕", "허먼 멜빌", "url",
        "DAILY", 1, new BigDecimal("4.5"), 10L, new BigDecimal("4.50"), Instant.now());
    PageResponse<BookRankingDto> response =
        new PageResponse<>(List.of(dto), null, null, 1, null, false);

    when(bookRankingService.getRankings(eq(PeriodType.DAILY), any(), any(), eq(10)))
        .thenReturn(response);


    //when & then
    mockMvc.perform(get("/api/books/popular")
            .param("period", "DAILY")
            .param("limit", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].title").value("모비 딕"))
        .andExpect(jsonPath("$.content[0].rank").value(1))
        .andExpect(jsonPath("$.hasNext").value(false));
  }

  @Test
  @DisplayName("limit이 범위를 벗어나면 400을 반환한다")
  void getRankings_invalidLimit_returns400() throws Exception {
    mockMvc.perform(get("/api/books/popular")
            .param("period", "DAILY")
            .param("limit", "0"))      // @Min(1) 위반
        .andExpect(status().isBadRequest());
  }
}
