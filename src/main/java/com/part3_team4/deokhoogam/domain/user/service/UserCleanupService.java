package com.part3_team4.deokhoogam.domain.user.service;

import com.part3_team4.deokhoogam.domain.user.repository.DeletedUserRepository;
import com.part3_team4.deokhoogam.domain.user.repository.UserRepository;
import java.time.Instant;import java.time.temporal.ChronoUnit;import java.util.List;import java.util.UUID;import org.springframework.scheduling.annotation.Scheduled;import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserCleanupService {

  private final DeletedUserRepository deletedUserRepository;
  private final UserRepository userRepository;

  public UserCleanupService(DeletedUserRepository deletedUserRepository, UserRepository userRepository) {
    this.deletedUserRepository = deletedUserRepository;
    this.userRepository = userRepository;
  }

  @Transactional
  @Scheduled(cron = "0 0 3 * * *")
  public void cleanupOldDeletedUsers() {
    Instant oneDayAgo = Instant.now().minus(1, ChronoUnit.DAYS);

    List<UUID> oldUserIds = deletedUserRepository.findUserIdsDeletedBefore(oneDayAgo);

    for (UUID id : oldUserIds) {
      userRepository.hardDeleteById(id);
    }
  }
}
