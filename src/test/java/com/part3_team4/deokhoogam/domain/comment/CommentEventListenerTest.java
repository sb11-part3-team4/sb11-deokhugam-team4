package com.part3_team4.deokhoogam.domain.comment;

import static org.mockito.Mockito.*;

import com.part3_team4.deokhoogam.domain.comment.entity.Comment;
import com.part3_team4.deokhoogam.domain.comment.repository.CommentRepository;
import com.part3_team4.deokhoogam.domain.comment.repository.DeletedCommentRepository;
import com.part3_team4.deokhoogam.domain.comment.service.CommentEventListener;
import com.part3_team4.deokhoogam.domain.user.entity.UserDeletedEvent;
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

  @Test
  @DisplayName("유저 하드 삭제 시 백업 데이터까지 삭제 확인")
  void handleUserDeletedEvent_HardDelete() {
    UUID userId = UUID.randomUUID();
    when(commentRepository.findAllByUserId(userId)).thenReturn(List.of(mock(Comment.class)));

    commentEventListener.handleUserDeletedEvent(new UserDeletedEvent(userId, true));

    verify(deletedCommentRepository).deleteByUserId(userId); // 백업 삭제 호출 확인
    verify(commentRepository).deleteAll(anyList()); // 원본 삭제 호출 확인
  }
}