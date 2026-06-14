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
  @DisplayName("유저 물리 삭제 시 랭킹 데이터가 정상적으로 삭제")
  void handleUserDeletedEvent_HardDelete() {
    UUID userId = UUID.randomUUID();

    // 물리 삭제 (isHardDelete = true)
    powerUserRankingEventListener.handleUserDeletedEvent(new UserDeletedEvent(userId, true));

    verify(powerUserRankingRepository, times(1)).deleteByUserId(userId);
  }

  @Test
  @DisplayName("유저 논리 삭제 시에도 랭킹 데이터가 정상적으로 삭제")
  void handleUserDeletedEvent_SoftDelete() {
    UUID userId = UUID.randomUUID();

    // 논리 삭제 (isHardDelete = false)
    powerUserRankingEventListener.handleUserDeletedEvent(new UserDeletedEvent(userId, false));

    verify(powerUserRankingRepository, times(1)).deleteByUserId(userId);
  }
}