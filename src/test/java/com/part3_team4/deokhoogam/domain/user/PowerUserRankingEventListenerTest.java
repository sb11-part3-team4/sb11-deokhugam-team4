package com.part3_team4.deokhoogam.domain.user;

import static org.mockito.Mockito.*;

import com.part3_team4.deokhoogam.domain.user.entity.UserDeletedEvent;
import com.part3_team4.deokhoogam.domain.user.repository.PowerUserRankingRepository;
import com.part3_team4.deokhoogam.domain.user.service.PowerUserRankingEventListener;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PowerUserRankingEventListenerTest {

  @InjectMocks
  private PowerUserRankingEventListener powerUserRankingEventListener;

  @Mock
  private PowerUserRankingRepository powerUserRankingRepository;

  @Test
  @DisplayName("유저 삭제 이벤트 발생 시 랭킹 데이터 삭제")
  void handleUserDeletedEvent() {
    UUID userId = UUID.randomUUID();

    powerUserRankingEventListener.handleUserDeletedEvent(new UserDeletedEvent(userId, true));

    verify(powerUserRankingRepository, times(1)).deleteByUserId(userId);
  }
}