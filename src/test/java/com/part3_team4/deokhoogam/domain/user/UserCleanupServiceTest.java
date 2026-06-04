package com.part3_team4.deokhoogam.domain.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.part3_team4.deokhoogam.domain.user.repository.DeletedUserRepository;
import com.part3_team4.deokhoogam.domain.user.repository.UserRepository;
import com.part3_team4.deokhoogam.domain.user.service.UserCleanupService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UserCleanupServiceTest {

  @InjectMocks
  private UserCleanupService userCleanupService;

  @Mock
  private DeletedUserRepository deletedUserRepository;

  @Mock
  private UserRepository userRepository;

  @Test
  @DisplayName("논리 삭제 후 1일 지난 유저 물리 삭제")
  void cleanupOldDeletedUsers_success() {
    UUID oldUserId1 = UUID.randomUUID();
    UUID oldUserId2 = UUID.randomUUID();
    List<UUID> targetUserIds = List.of(oldUserId1, oldUserId2);

    given(deletedUserRepository.findUserIdsDeletedBefore(any(Instant.class)))
        .willReturn(targetUserIds);

    userCleanupService.cleanupOldDeletedUsers();

    then(deletedUserRepository).should(times(1))
        .findUserIdsDeletedBefore(any(Instant.class));

    then(userRepository).should(times(1))
        .hardDeleteById(oldUserId1);
    then(userRepository).should(times(1))
        .hardDeleteById(oldUserId2);
  }

  @Test
  @DisplayName("삭제 대상이 없으면 물리 삭제 메서드가 호출되지 않음")
  void cleanupOldDeletedUsers_emptyList() {
    given(deletedUserRepository.findUserIdsDeletedBefore(any(Instant.class)))
        .willReturn(List.of());

    userCleanupService.cleanupOldDeletedUsers();

    then(deletedUserRepository).should(times(1))
        .findUserIdsDeletedBefore(any(Instant.class));
    then(userRepository).should(never()).hardDeleteById(any());
  }
}
