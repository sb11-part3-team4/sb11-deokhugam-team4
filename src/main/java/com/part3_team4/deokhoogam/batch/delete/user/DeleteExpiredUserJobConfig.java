package com.part3_team4.deokhoogam.batch.delete.user;

import com.part3_team4.deokhoogam.domain.user.service.DeletedUserAutoDeleteService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeleteExpiredUserJobConfig {

  private final DeletedUserAutoDeleteService deletedUserAutoDeleteService;

  @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
  public void deleteExpiredUsers() {
    deletedUserAutoDeleteService.deleteExpiredUsers();
  }
}