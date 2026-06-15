package com.part3_team4.deokhoogam.domain.notification;

import static org.mockito.Mockito.*;

import com.part3_team4.deokhoogam.domain.notification.repository.NotificationRepository;
import com.part3_team4.deokhoogam.domain.notification.service.NotificationEventListener;
import com.part3_team4.deokhoogam.domain.review.entity.ReviewDeletedEvent;
import com.part3_team4.deokhoogam.domain.user.entity.UserDeletedEvent;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

  @InjectMocks
  private NotificationEventListener notificationEventListener;

  @Mock
  private NotificationRepository notificationRepository;

  @Test
  @DisplayName("리뷰 삭제 이벤트 수신 시 리뷰 관련 알림 삭제 로직이 호출")
  void handleReviewDeletedEvent() {
    UUID reviewId = UUID.randomUUID();

    notificationEventListener.handleReviewDeletedEvent(new ReviewDeletedEvent(reviewId, true));

    verify(notificationRepository, times(1)).deleteAllByReviewId(reviewId);
  }

  @Test
  @DisplayName("유저 삭제 이벤트 수신 시 유저 관련 알림 삭제 로직이 호출")
  void handleUserDeletedEvent() {
    UUID userId = UUID.randomUUID();

    notificationEventListener.handleUserDeletedEvent(new UserDeletedEvent(userId, true));

    verify(notificationRepository, times(1)).deleteAllByUserId(userId);
  }
}