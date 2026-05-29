package com.part3_team4.deokhoogam.domain.comment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.part3_team4.deokhoogam.domain.comment.controller.CommentController;
import com.part3_team4.deokhoogam.domain.comment.dto.CommentDto;
import com.part3_team4.deokhoogam.domain.comment.exception.CommentNotFoundException;
import com.part3_team4.deokhoogam.domain.comment.exception.CommentNotOwnerException;
import com.part3_team4.deokhoogam.domain.comment.service.CommentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CommentController.class)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CommentService commentService;

    private static final String USER_HEADER = "Deokhugam-Request-User-ID";
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID REVIEW_ID = UUID.randomUUID();
    private static final UUID COMMENT_ID = UUID.randomUUID();

    private CommentDto.CommentResponse sampleResponse(String content) {
        return new CommentDto.CommentResponse(
                COMMENT_ID, REVIEW_ID, USER_ID, content, Instant.now(), Instant.now());
    }

    // ─────────────────────────────────────────────
    // 댓글 등록 POST /api/comments
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("댓글 등록 POST /api/comments")
    class CreateComment {

        @Test
        @DisplayName("댓글을 정상적으로 등록하면 201을 반환한다")
        void createComment_success_returns201() throws Exception {
            String content = "정말 좋은 리뷰입니다.";
            given(commentService.createComment(any(UUID.class), any(UUID.class), any(String.class)))
                    .willReturn(sampleResponse(content));

            mockMvc.perform(post("/api/comments")
                            .header(USER_HEADER, USER_ID.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "reviewId", REVIEW_ID.toString(),
                                    "content", content))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.content").value(content))
                    .andExpect(jsonPath("$.reviewId").value(REVIEW_ID.toString()))
                    .andExpect(jsonPath("$.userId").value(USER_ID.toString()));
        }

        @Test
        @DisplayName("헤더에 유저 ID가 없으면 400을 반환한다")
        void createComment_missingUserIdHeader_returns400() throws Exception {
            mockMvc.perform(post("/api/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "reviewId", REVIEW_ID.toString(),
                                    "content", "내용"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("내용이 비어있으면 400을 반환한다")
        void createComment_emptyContent_returns400() throws Exception {
            mockMvc.perform(post("/api/comments")
                            .header(USER_HEADER, USER_ID.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "reviewId", REVIEW_ID.toString(),
                                    "content", ""))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("reviewId가 없으면 400을 반환한다")
        void createComment_missingReviewId_returns400() throws Exception {
            mockMvc.perform(post("/api/comments")
                            .header(USER_HEADER, USER_ID.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "content", "내용"))))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─────────────────────────────────────────────
    // 댓글 목록 조회 GET /api/comments
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("댓글 목록 조회 GET /api/comments")
    class GetComments {

        @Test
        @DisplayName("reviewId로 댓글 목록을 정상 조회하면 200을 반환한다")
        void getComments_success_returns200() throws Exception {
            given(commentService.getComments(any(UUID.class), isNull(), anyInt()))
                    .willReturn(List.of(sampleResponse("댓글 내용")));

            mockMvc.perform(get("/api/comments")
                            .param("reviewId", REVIEW_ID.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].content").value("댓글 내용"));
        }

        @Test
        @DisplayName("cursor 없이 조회하면 첫 페이지를 반환한다")
        void getComments_withoutCursor_returnsFirstPage() throws Exception {
            given(commentService.getComments(any(UUID.class), isNull(), anyInt()))
                    .willReturn(List.of(sampleResponse("첫 번째 댓글"), sampleResponse("두 번째 댓글")));

            mockMvc.perform(get("/api/comments")
                            .param("reviewId", REVIEW_ID.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("cursor와 함께 조회하면 해당 커서 이후 데이터를 반환한다")
        void getComments_withCursor_returnsDataAfterCursor() throws Exception {
            given(commentService.getComments(any(UUID.class), any(Instant.class), anyInt()))
                    .willReturn(List.of(sampleResponse("커서 이후 댓글")));

            mockMvc.perform(get("/api/comments")
                            .param("reviewId", REVIEW_ID.toString())
                            .param("cursor", Instant.now().minusSeconds(10).toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].content").value("커서 이후 댓글"));
        }

        @Test
        @DisplayName("잘못된 cursor 형식으로 요청하면 400을 반환한다")
        void getComments_invalidCursor_returns400() throws Exception {
            mockMvc.perform(get("/api/comments")
                            .param("reviewId", REVIEW_ID.toString())
                            .param("cursor", "not-a-valid-instant"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─────────────────────────────────────────────
    // 댓글 상세 조회 GET /api/comments/{commentId}
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("댓글 상세 조회 GET /api/comments/{commentId}")
    class GetComment {

        @Test
        @DisplayName("정상적으로 댓글 상세를 조회하면 200을 반환한다")
        void getComment_success_returns200() throws Exception {
            given(commentService.getComment(any(UUID.class)))
                    .willReturn(sampleResponse("상세 조회 댓글"));

            mockMvc.perform(get("/api/comments/{commentId}", COMMENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(COMMENT_ID.toString()))
                    .andExpect(jsonPath("$.content").value("상세 조회 댓글"));
        }

        @Test
        @DisplayName("존재하지 않는 commentId로 조회하면 404를 반환한다")
        void getComment_notFound_returns404() throws Exception {
            given(commentService.getComment(any(UUID.class)))
                    .willThrow(CommentNotFoundException.withId(COMMENT_ID));

            mockMvc.perform(get("/api/comments/{commentId}", COMMENT_ID))
                    .andExpect(status().isNotFound());
        }
    }

    // ─────────────────────────────────────────────
    // 댓글 수정 PATCH /api/comments/{commentId}
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("댓글 수정 PATCH /api/comments/{commentId}")
    class UpdateComment {

        @Test
        @DisplayName("본인 댓글을 정상적으로 수정하면 200을 반환한다")
        void updateComment_success_returns200() throws Exception {
            String updatedContent = "수정된 댓글 내용입니다.";
            given(commentService.updateComment(any(UUID.class), any(UUID.class), any(String.class)))
                    .willReturn(sampleResponse(updatedContent));

            mockMvc.perform(patch("/api/comments/{commentId}", COMMENT_ID)
                            .header(USER_HEADER, USER_ID.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "content", updatedContent))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").value(updatedContent));
        }

        @Test
        @DisplayName("헤더에 유저 ID가 없으면 400을 반환한다")
        void updateComment_missingUserIdHeader_returns400() throws Exception {
            mockMvc.perform(patch("/api/comments/{commentId}", COMMENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("content", "수정 내용"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("빈 content로 수정하면 400을 반환한다")
        void updateComment_emptyContent_returns400() throws Exception {
            mockMvc.perform(patch("/api/comments/{commentId}", COMMENT_ID)
                            .header(USER_HEADER, USER_ID.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("content", ""))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("다른 사람 댓글을 수정하면 403을 반환한다")
        void updateComment_notOwner_returns403() throws Exception {
            given(commentService.updateComment(any(UUID.class), any(UUID.class), any(String.class)))
                    .willThrow(CommentNotOwnerException.forUser(USER_ID));

            mockMvc.perform(patch("/api/comments/{commentId}", COMMENT_ID)
                            .header(USER_HEADER, UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("content", "수정 시도"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("존재하지 않는 댓글을 수정하면 404를 반환한다")
        void updateComment_notFound_returns404() throws Exception {
            given(commentService.updateComment(any(UUID.class), any(UUID.class), any(String.class)))
                    .willThrow(CommentNotFoundException.withId(COMMENT_ID));

            mockMvc.perform(patch("/api/comments/{commentId}", COMMENT_ID)
                            .header(USER_HEADER, USER_ID.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("content", "수정 시도"))))
                    .andExpect(status().isNotFound());
        }
    }

    // ─────────────────────────────────────────────
    // 댓글 논리 삭제 DELETE /api/comments/{commentId}
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("댓글 논리 삭제 DELETE /api/comments/{commentId}")
    class SoftDeleteComment {

        @Test
        @DisplayName("본인 댓글을 정상적으로 논리 삭제하면 204를 반환한다")
        void softDeleteComment_success_returns204() throws Exception {
            willDoNothing().given(commentService).softDeleteComment(any(UUID.class), any(UUID.class));

            mockMvc.perform(delete("/api/comments/{commentId}", COMMENT_ID)
                            .header(USER_HEADER, USER_ID.toString()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("헤더에 유저 ID가 없으면 400을 반환한다")
        void softDeleteComment_missingUserIdHeader_returns400() throws Exception {
            mockMvc.perform(delete("/api/comments/{commentId}", COMMENT_ID))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("다른 사람 댓글을 삭제하면 403을 반환한다")
        void softDeleteComment_notOwner_returns403() throws Exception {
            willThrow(CommentNotOwnerException.forUser(USER_ID))
                    .given(commentService).softDeleteComment(any(UUID.class), any(UUID.class));

            mockMvc.perform(delete("/api/comments/{commentId}", COMMENT_ID)
                            .header(USER_HEADER, UUID.randomUUID().toString()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("존재하지 않는 댓글을 삭제하면 404를 반환한다")
        void softDeleteComment_notFound_returns404() throws Exception {
            willThrow(CommentNotFoundException.withId(COMMENT_ID))
                    .given(commentService).softDeleteComment(any(UUID.class), any(UUID.class));

            mockMvc.perform(delete("/api/comments/{commentId}", COMMENT_ID)
                            .header(USER_HEADER, USER_ID.toString()))
                    .andExpect(status().isNotFound());
        }
    }

    // ─────────────────────────────────────────────
    // 댓글 물리 삭제 DELETE /api/comments/{commentId}/hard
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("댓글 물리 삭제 DELETE /api/comments/{commentId}/hard")
    class HardDeleteComment {

        @Test
        @DisplayName("정상적으로 물리 삭제하면 204를 반환한다")
        void hardDeleteComment_success_returns204() throws Exception {
            willDoNothing().given(commentService).hardDeleteComment(any(UUID.class));

            mockMvc.perform(delete("/api/comments/{commentId}/hard", COMMENT_ID))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("존재하지 않는 댓글을 물리 삭제하면 404를 반환한다")
        void hardDeleteComment_notFound_returns404() throws Exception {
            willThrow(CommentNotFoundException.withId(COMMENT_ID))
                    .given(commentService).hardDeleteComment(any(UUID.class));

            mockMvc.perform(delete("/api/comments/{commentId}/hard", COMMENT_ID))
                    .andExpect(status().isNotFound());
        }
    }
}
