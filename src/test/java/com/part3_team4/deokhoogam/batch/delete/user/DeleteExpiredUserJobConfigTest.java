package com.part3_team4.deokhoogam.batch.delete.user;

import static org.mockito.BDDMockito.then;

import com.part3_team4.deokhoogam.domain.user.service.DeletedUserAutoDeleteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeleteExpiredUserJobConfigTest {

  @Mock
  private DeletedUserAutoDeleteService deletedUserAutoDeleteService;

  @InjectMocks
  private DeleteExpiredUserJobConfig deleteExpiredUserJobConfig;

  @Test
  @DisplayName("만료된 유저 삭제 배치가 실행되면 유저 자동 삭제 서비스를 호출")
  void deleteExpiredUsers() {
    deleteExpiredUserJobConfig.deleteExpiredUsers();

    then(deletedUserAutoDeleteService)
        .should()
        .deleteExpiredUsers();
  }
}