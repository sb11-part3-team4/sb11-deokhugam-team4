package com.part3_team4.deokhoogam.domain.comment.service;

import com.part3_team4.deokhoogam.domain.comment.entity.Comment;
import com.part3_team4.deokhoogam.domain.comment.entity.DeletedComment;
import com.part3_team4.deokhoogam.domain.comment.repository.CommentRepository;
import com.part3_team4.deokhoogam.domain.comment.repository.DeletedCommentRepository;
import com.part3_team4.deokhoogam.domain.review.entity.ReviewDeletedEvent;
import com.part3_team4.deokhoogam.domain.user.entity.UserDeletedEvent;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommentEventListener {

  private final CommentRepository commentRepository;
  private final DeletedCommentRepository deletedCommentRepository;

  // 유저 삭제 이벤트 수신 (작성자가 탈퇴했을 때)
  @EventListener
  public void handleUserDeletedEvent(UserDeletedEvent event) {
    log.info("유저 삭제 이벤트 수신 (댓글 파트) - userId: {}, isHardDelete: {}", event.userId(), event.isHardDelete());

    List<Comment> comments = commentRepository.findAllByUserId(event.userId());
    if (comments.isEmpty()) return;

    if (!event.isHardDelete()) {
      // 백업 테이블로 복사
      List<DeletedComment> deletedComments = comments.stream()
          .map(DeletedComment::from)
          .toList();
      deletedCommentRepository.saveAll(deletedComments);
    }

    // 테이블에서 삭제
    commentRepository.deleteAll(comments);
  }

  // 리뷰 삭제 이벤트 수신 (상위 리뷰가 삭제되었을 때)
  @EventListener
  public void handleReviewDeletedEvent(ReviewDeletedEvent event) {
    log.info("리뷰 삭제 이벤트 수신 (댓글 파트) - reviewId: {}, isHardDelete: {}", event.reviewId(), event.isHardDelete());

    List<Comment> comments = commentRepository.findAllByReviewId(event.reviewId());
    if (comments.isEmpty()) return;

    if (!event.isHardDelete()) {
      // 백업 테이블로 복사
      List<DeletedComment> deletedComments = comments.stream()
          .map(DeletedComment::from)
          .toList();
      deletedCommentRepository.saveAll(deletedComments);
    }

    // 테이블에서 삭제
    commentRepository.deleteAll(comments);
  }
}