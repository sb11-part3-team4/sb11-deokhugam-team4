package com.part3_team4.deokhoogam.domain.user.service;

import com.part3_team4.deokhoogam.domain.user.repository.DeletedUserRepository;
import com.part3_team4.deokhoogam.domain.user.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class UserCleanupService {

  private final DeletedUserRepository deletedUserRepository;
  private final UserRepository userRepository;
  private static final Logger log = LoggerFactory.getLogger(UserCleanupService.class);

  public UserCleanupService(DeletedUserRepository deletedUserRepository, UserRepository userRepository) {
    this.deletedUserRepository = deletedUserRepository;
    this.userRepository = userRepository;
  }

  @Scheduled(cron = "0 0 3 * * *")
  public void cleanupOldDeletedUsers() {
    Instant oneDayAgo = Instant.now().minus(1, ChronoUnit.DAYS);

    List<UUID> oldUserIds = deletedUserRepository.findUserIdsDeletedBefore(oneDayAgo);

    for (UUID id : oldUserIds) {
      try {
        userRepository.hardDeleteById(id);

        deletedUserRepository.deleteById(id);
      } catch (Exception e) {
        log.error("유저 영구 삭제 실패 (ID: {}): {}", id, e.getMessage());
      }
    }
  }
}
