package com.part3_team4.deokhoogam.domain.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.part3_team4.deokhoogam.domain.user.entity.DeletedUser;
import com.part3_team4.deokhoogam.domain.user.repository.DeletedUserRepository;
import com.part3_team4.deokhoogam.domain.user.service.DeletedUserAutoDeleteService;
import com.part3_team4.deokhoogam.global.metric.CustomMetrics;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class DeletedUserAutoDeleteServiceTest {

  @Mock
  private DeletedUserRepository deletedUserRepository;

  @Mock
  private Clock clock; // 테스트 시점의 시간을 고정하기 위한 Mock

  @Mock
  private CustomMetrics customMetrics;

  @InjectMocks
  private DeletedUserAutoDeleteService deletedUserAutoDeleteService;

  @Test
  @DisplayName("1일 지난 유저 백업 테이블에서 삭제 성공")
  void deleteExpiredUsers_success() {
    Instant fixedNow = Instant.parse("2026-06-08T00:00:00Z");
    given(clock.instant()).willReturn(fixedNow);

    DeletedUser oldUser1 = mock(DeletedUser.class);
    DeletedUser oldUser2 = mock(DeletedUser.class);

    // 첫 번째 페이지 호출 시 2명 반환, 두 번째 호출 시 빈 리스트 반환 (무한 루프 종료)
    given(deletedUserRepository.findByDeletedAtBefore(any(Instant.class), any(Pageable.class)))
        .willReturn(List.of(oldUser1, oldUser2))
        .willReturn(List.of());

    deletedUserAutoDeleteService.deleteExpiredUsers();

    then(deletedUserRepository).should(times(2))
        .findByDeletedAtBefore(any(Instant.class), any(Pageable.class));
    then(deletedUserRepository).should(times(1)).delete(oldUser1);
    then(deletedUserRepository).should(times(1)).delete(oldUser2);
  }

  @Test
  @DisplayName("삭제 대상이 없으면 영구 삭제 메서드가 호출되지 않음")
  void deleteExpiredUsers_emptyList() {
    Instant fixedNow = Instant.parse("2026-06-08T00:00:00Z");
    given(clock.instant()).willReturn(fixedNow);

    given(deletedUserRepository.findByDeletedAtBefore(any(Instant.class), any(Pageable.class)))
        .willReturn(List.of());

    deletedUserAutoDeleteService.deleteExpiredUsers();

    then(deletedUserRepository).should(times(1))
        .findByDeletedAtBefore(any(Instant.class), any(Pageable.class));
    then(deletedUserRepository).should(never()).delete(any(DeletedUser.class));
  }

  @Test
  @DisplayName("유저 영구 삭제 중 예외가 발생해도 다른 유저 삭제는 진행")
  void deleteExpiredUsers_exception_continue() {
    // given
    Instant fixedNow = Instant.parse("2026-06-08T00:00:00Z");
    given(clock.instant()).willReturn(fixedNow);

    DeletedUser errorUser = mock(DeletedUser.class);
    DeletedUser successUser = mock(DeletedUser.class);

    given(deletedUserRepository.findByDeletedAtBefore(any(Instant.class), any(Pageable.class)))
        .willReturn(List.of(errorUser, successUser))
        .willReturn(List.of());

    // 에러 유저 삭제 시 예외 발생
    willThrow(new RuntimeException("DB 삭제 에러 발생 테스트"))
        .given(deletedUserRepository).delete(errorUser);

    deletedUserAutoDeleteService.deleteExpiredUsers();

    then(deletedUserRepository).should(times(1)).delete(errorUser);
    then(deletedUserRepository).should(times(1)).delete(successUser);
  }
}