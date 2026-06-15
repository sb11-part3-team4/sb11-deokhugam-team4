package com.part3_team4.deokhoogam.domain.comment;

import static org.mockito.Mockito.*;

import com.part3_team4.deokhoogam.domain.comment.entity.Comment;
import com.part3_team4.deokhoogam.domain.comment.repository.CommentRepository;
import com.part3_team4.deokhoogam.domain.comment.repository.DeletedCommentRepository;
import com.part3_team4.deokhoogam.domain.comment.service.CommentEventListener;
import com.part3_team4.deokhoogam.domain.review.entity.ReviewDeletedEvent;
import com.part3_team4.deokhoogam.domain.user.entity.UserDeletedEvent;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommentEventListenerTest {

  @InjectMocks
  private CommentEventListener commentEventListener;

  @Mock private CommentRepository commentRepository;
  @Mock private DeletedCommentRepository deletedCommentRepository;

  // 유저 삭제 이벤트 테스트

  @Test
  @DisplayName("유저 하드 삭제 시 백업 데이터를 삭제하고 원본 댓글도 삭제")
  void handleUserDeletedEvent_HardDelete() {
    UUID userId = UUID.randomUUID();
    when(commentRepository.findAllByUserId(userId)).thenReturn(List.of(mock(Comment.class)));

    commentEventListener.handleUserDeletedEvent(new UserDeletedEvent(userId, true));

    verify(deletedCommentRepository, times(1)).deleteByUserId(userId);
    verify(deletedCommentRepository, never()).saveAll(any()); // 하드 삭제는 백업 안 함
    verify(commentRepository, times(1)).deleteAll(anyList());
  }

  @Test
  @DisplayName("유저 소프트 삭제 시 백업 데이터를 지우지 않고 원본을 백업 후 삭제")
  void handleUserDeletedEvent_SoftDelete() {
    UUID userId = UUID.randomUUID();
    when(commentRepository.findAllByUserId(userId)).thenReturn(List.of(mock(Comment.class)));

    commentEventListener.handleUserDeletedEvent(new UserDeletedEvent(userId, false));

    verify(deletedCommentRepository, never()).deleteByUserId(any()); // 소프트 삭제는 기존 백업 지우지 않음
    verify(deletedCommentRepository, times(1)).saveAll(anyList()); // 백업 진행
    verify(commentRepository, times(1)).deleteAll(anyList());
  }

  @Test
  @DisplayName("유저 삭제 시 작성한 댓글이 없으면 로직을 조기 종료")
  void handleUserDeletedEvent_EmptyComments() {
    UUID userId = UUID.randomUUID();
    when(commentRepository.findAllByUserId(userId)).thenReturn(Collections.emptyList());

    // 물리 삭제로 테스트하여 deleteByUserId는 실행되는지, 그 이후 로직은 멈추는지 검증
    commentEventListener.handleUserDeletedEvent(new UserDeletedEvent(userId, true));

    verify(deletedCommentRepository, times(1)).deleteByUserId(userId); // 처음 검증은 통과
    verify(deletedCommentRepository, never()).saveAll(any()); // 조기 종료되어야 함
    verify(commentRepository, never()).deleteAll(any()); // 조기 종료되어야 함
  }


  // 리뷰 삭제 이벤트 테스트

  @Test
  @DisplayName("리뷰 하드 삭제 시 백업 데이터를 삭제하고 원본 댓글도 삭제")
  void handleReviewDeletedEvent_HardDelete() {
    UUID reviewId = UUID.randomUUID();
    when(commentRepository.findAllByReviewId(reviewId)).thenReturn(List.of(mock(Comment.class)));

    commentEventListener.handleReviewDeletedEvent(new ReviewDeletedEvent(reviewId, true));

    verify(deletedCommentRepository, times(1)).deleteByReviewId(reviewId);
    verify(deletedCommentRepository, never()).saveAll(any());
    verify(commentRepository, times(1)).deleteAll(anyList());
  }

  @Test
  @DisplayName("리뷰 소프트 삭제 시 백업 데이터를 지우지 않고 원본을 백업 후 삭제")
  void handleReviewDeletedEvent_SoftDelete() {
    UUID reviewId = UUID.randomUUID();
    when(commentRepository.findAllByReviewId(reviewId)).thenReturn(List.of(mock(Comment.class)));

    commentEventListener.handleReviewDeletedEvent(new ReviewDeletedEvent(reviewId, false));

    verify(deletedCommentRepository, never()).deleteByReviewId(any());
    verify(deletedCommentRepository, times(1)).saveAll(anyList());
    verify(commentRepository, times(1)).deleteAll(anyList());
  }

  @Test
  @DisplayName("리뷰 삭제 시 작성된 댓글이 없으면 로직을 조기 종료")
  void handleReviewDeletedEvent_EmptyComments() {
    UUID reviewId = UUID.randomUUID();
    when(commentRepository.findAllByReviewId(reviewId)).thenReturn(Collections.emptyList());

    commentEventListener.handleReviewDeletedEvent(new ReviewDeletedEvent(reviewId, true));

    verify(deletedCommentRepository, times(1)).deleteByReviewId(reviewId);
    verify(deletedCommentRepository, never()).saveAll(any());
    verify(commentRepository, never()).deleteAll(any());
  }
}