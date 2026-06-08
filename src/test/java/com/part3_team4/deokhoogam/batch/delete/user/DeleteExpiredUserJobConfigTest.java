package com.part3_team4.deokhoogam.batch.delete.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.part3_team4.deokhoogam.domain.user.entity.DeletedUser;
import com.part3_team4.deokhoogam.domain.user.repository.DeletedUserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class DeleteExpiredUserJobConfigTest {

  private DeleteExpiredUserJobConfig deleteExpiredUserJobConfig;

  @Mock
  private DeletedUserRepository deletedUserRepository;

  //테스트가 언제 실행되든 항상 같은 시간을 반환하도록 고정된 시계를 생성
  private final Instant FIXED_NOW = Instant.parse("2026-06-08T03:00:00Z");
  private final Clock fixedClock = Clock.fixed(FIXED_NOW, ZoneId.of("UTC"));

  @BeforeEach
  void setUp() {
    deleteExpiredUserJobConfig = new DeleteExpiredUserJobConfig(deletedUserRepository, fixedClock);
  }

  @Test
  @DisplayName("1일 지난 유저 백업 테이블에서 삭제 성공")
  void deleteExpiredUsers_success() {
    DeletedUser oldUser1 = mock(DeletedUser.class);
    DeletedUser oldUser2 = mock(DeletedUser.class);

    given(deletedUserRepository.findByDeletedAtBefore(any(Instant.class), any(Pageable.class)))
        .willReturn(List.of(oldUser1, oldUser2))
        .willReturn(List.of());

    deleteExpiredUserJobConfig.deleteExpiredUsers();

    then(deletedUserRepository).should(times(2))
        .findByDeletedAtBefore(any(Instant.class), any(Pageable.class));
    then(deletedUserRepository).should(times(1)).delete(oldUser1);
    then(deletedUserRepository).should(times(1)).delete(oldUser2);
  }

  @Test
  @DisplayName("삭제 대상이 없으면 영구 삭제 메서드가 호출되지 않음")
  void deleteExpiredUsers_emptyList() {
    given(deletedUserRepository.findByDeletedAtBefore(any(Instant.class), any(Pageable.class)))
        .willReturn(List.of());

    deleteExpiredUserJobConfig.deleteExpiredUsers();

    then(deletedUserRepository).should(times(1))
        .findByDeletedAtBefore(any(Instant.class), any(Pageable.class));
    then(deletedUserRepository).should(never()).delete(any());
  }

  @Test
  @DisplayName("유저 영구 삭제 중 예외가 발생해도 다른 유저 삭제는 진행")
  void deleteExpiredUsers_exception_continue() {
    DeletedUser errorUser = mock(DeletedUser.class);
    DeletedUser successUser = mock(DeletedUser.class);

    given(deletedUserRepository.findByDeletedAtBefore(any(Instant.class), any(Pageable.class)))
        .willReturn(List.of(errorUser, successUser))
        .willReturn(List.of());

    willThrow(new RuntimeException("DB 삭제 에러 발생 테스트"))
        .given(deletedUserRepository).delete(errorUser);

    deleteExpiredUserJobConfig.deleteExpiredUsers();

    then(deletedUserRepository).should(times(1)).delete(errorUser);
    then(deletedUserRepository).should(times(1)).delete(successUser);
  }
}