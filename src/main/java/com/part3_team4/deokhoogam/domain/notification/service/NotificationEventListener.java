package com.part3_team4.deokhoogam.domain.notification.service;

import com.part3_team4.deokhoogam.domain.notification.repository.NotificationRepository;
import com.part3_team4.deokhoogam.domain.review.entity.ReviewDeletedEvent;
import com.part3_team4.deokhoogam.domain.user.entity.UserDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

  private final NotificationRepository notificationRepository;

  //리뷰 삭제 이벤트 수신 시 연관된 알림 일괄 삭제
  @EventListener
  @Transactional
  public void handleReviewDeletedEvent(ReviewDeletedEvent event) {
    log.info("리뷰 삭제 이벤트 수신 (알림 파트) - reviewId: {}", event.reviewId());
    notificationRepository.deleteAllByReviewId(event.reviewId());
  }

  //유저 삭제 이벤트 수신 시 연관된 알림 일괄 삭제
  @EventListener
  @Transactional
  public void handleUserDeletedEvent(UserDeletedEvent event) {
    log.info("유저 삭제 이벤트 수신 (알림 파트) - userId: {}", event.userId());
    notificationRepository.deleteAllByUserId(event.userId());
  }
}