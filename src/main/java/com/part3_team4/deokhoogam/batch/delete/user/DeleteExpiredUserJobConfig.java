package com.part3_team4.deokhoogam.batch.delete.user;

import com.part3_team4.deokhoogam.domain.user.repository.DeletedUserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DeleteExpiredUserJobConfig {

  private final DeletedUserRepository deletedUserRepository;
  private final Clock clock;
  private static final Logger log = LoggerFactory.getLogger(DeleteExpiredUserJobConfig.class);

  public DeleteExpiredUserJobConfig(DeletedUserRepository deletedUserRepository, Clock clock) {
    this.deletedUserRepository = deletedUserRepository;
    this.clock = clock;
  }

  @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
  public void deleteExpiredUsers() {
    Instant oneDayAgo = Instant.now(clock).minus(1, ChronoUnit.DAYS);

    List<UUID> oldUserIds = deletedUserRepository.findUserIdsDeletedBefore(oneDayAgo);

    for (UUID id : oldUserIds) {
      try {
        deletedUserRepository.deleteById(id);
      } catch (Exception e) {
        log.error("유저 영구 삭제 실패 (ID: {}): {}", id, e.getMessage());
      }
    }
  }
}