package com.part3_team4.deokhoogam.batch.delete.user;

import com.part3_team4.deokhoogam.domain.user.entity.DeletedUser;
import com.part3_team4.deokhoogam.domain.user.repository.DeletedUserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    int pageSize = 100;

    while (true) {
      Pageable pageable = PageRequest.of(0, pageSize);

      List<DeletedUser> oldUsers = deletedUserRepository.findByDeletedAtBefore(oneDayAgo, pageable);

      if (oldUsers.isEmpty()) {
        break;
      }

      // 가져온 100개의 데이터를 순회하며 삭제
      for (DeletedUser oldUser : oldUsers) {
        try {
          deletedUserRepository.delete(oldUser);
        } catch (Exception e) {
          log.error("유저 영구 삭제 실패 (ID: {}): {}", oldUser.getId(), e.getMessage());
        }
      }
    }

    log.info("만료된 탈퇴 유저 영구 삭제 배치 작업 완료");
  }
}