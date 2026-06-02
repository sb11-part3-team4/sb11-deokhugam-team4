package com.part3_team4.deokhoogam.domain.review;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.part3_team4.deokhoogam.domain.book.exception.BookNotFoundException;
import com.part3_team4.deokhoogam.domain.review.controller.ReviewController;
import com.part3_team4.deokhoogam.domain.review.dto.ReviewCreateRequest;
import com.part3_team4.deokhoogam.domain.review.dto.ReviewResponse;
import com.part3_team4.deokhoogam.domain.review.exception.ReviewAlreadyExistsException;
import com.part3_team4.deokhoogam.domain.review.service.ReviewService;
import com.part3_team4.deokhoogam.global.jwt.JwtFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.http.MediaType;

@WebMvcTest(controllers = ReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
public class ReviewControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    ReviewService reviewService;

    @MockitoBean
    private JwtFilter jwtFilter;

    @Test
    @DisplayName("리뷰 등록 성공 시 201을 반환한다")
    void createdReview_success() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();

        ReviewCreateRequest request = new ReviewCreateRequest(bookId, 4, "종은 책이에요");

        ReviewResponse response = new ReviewResponse(
                reviewId, userId, bookId, 4, "좋은 책이에요", 0, 0, false, Instant.now(), Instant.now()
        );

        given(reviewService.createReview(eq(userId),
                any(ReviewCreateRequest.class)))
                .willReturn(response);

        mockMvc.perform(post("/api/reviews")
                        .header("Deokhugam-Request-User-ID", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(reviewId.toString()));

    }

    @Test
    @DisplayName("동일 도서에 이미 리뷰가 존재하면 409를 반환한다")
    void createdReview_duplicateReview_returns409() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();

        ReviewCreateRequest request = new ReviewCreateRequest(bookId, 4, "좋은 책이에요");

        given(reviewService.createReview(eq(userId),
                any(ReviewCreateRequest.class)))
                .willThrow(ReviewAlreadyExistsException.withUserIdAndBookId(userId, bookId));

        mockMvc.perform(post("/api/reviews")
                        .header("Deokhugam-Request-User-ID", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("없는 도서를 찾으려고 하면 404를 반환한다")
    void noFindbookId_false404() throws Exception {
        UUID bookId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        ReviewCreateRequest request = new ReviewCreateRequest(bookId, 4, "좋은 책이에요");

        given(reviewService.createReview(eq(userId),
                any(ReviewCreateRequest.class)))
                .willThrow(BookNotFoundException.withId(bookId));

        mockMvc.perform(post("/api/reviews")
                        .header("Deokhugam-Request-User-ID", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("리뷰 별점의 범위를 초과하면 400을 반환한다")
    void overchecking_rating() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        ReviewCreateRequest request = new ReviewCreateRequest(bookId, 6, "좋은 책이에요");

        mockMvc.perform(post("/api/reviews")
                        .header("Deokhugam-Request-User-ID", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("코멘트가 없으면 400을 반환한다")
    void no_Content() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();

        ReviewCreateRequest request = new ReviewCreateRequest(bookId, 4, "");

        mockMvc.perform(post("/api/reviews")
                        .header("Deokhugam-Request-User-ID", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

    }

    @Test
    @DisplayName("헤더가 없으면 400을 반환한다")
    void createReview_missingHeader_returns400() throws Exception {
        UUID bookId = UUID.randomUUID();
        ReviewCreateRequest request = new ReviewCreateRequest(bookId, 4, "좋은 책이에요");

        mockMvc.perform(post("/api/reviews")
                        // header 없음
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("헤더가 없으면 400을 반환한다")
    void getReview_missingHeader_returns400() throws Exception {
        UUID reviewId = UUID.randomUUID();

        mockMvc.perform(get("/api/reviews/" + reviewId))
                .andExpect(status().isBadRequest());

    }

    @Test
    @DisplayName("리뷰 상세 조회 성공시 200과 likedByMe를 반환한다")
    void getReview_success() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();

        ReviewResponse response = new ReviewResponse(
                reviewId, userId, bookId, 4, "좋은 책이에요", 0, 0, true, Instant.now(), Instant.now()
                );

        given(reviewService.getReview(eq(reviewId), eq(userId))).willReturn(response);

        mockMvc.perform(get("/api/reviews/" + reviewId)
                .header("Deokhugam-Request-User-ID", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likedByMe").value(true));
    }


}
