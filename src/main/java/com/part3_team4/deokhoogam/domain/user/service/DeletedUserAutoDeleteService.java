package com.part3_team4.deokhoogam.domain.user.service;

import com.part3_team4.deokhoogam.domain.user.entity.DeletedUser;
import com.part3_team4.deokhoogam.domain.user.repository.DeletedUserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeletedUserAutoDeleteService {
  private final DeletedUserRepository deletedUserRepository;
  private final Clock clock;

  @Transactional
  public void deleteExpiredUsers() {
    Instant oneDayAgo = Instant.now(clock).minus(1, ChronoUnit.DAYS);
    int pageSize = 100;

    while (true) {
      Pageable pageable = PageRequest.of(0, pageSize);
      List<DeletedUser> oldUsers = deletedUserRepository.findByDeletedAtBefore(oneDayAgo, pageable);

      if (oldUsers.isEmpty()) {
        break;
      }

      for (DeletedUser oldUser : oldUsers) {
        try {
          deletedUserRepository.delete(oldUser);
        } catch (Exception e) {
          // 의도적인 빈 블록:
          // 특정 유저의 데이터 삭제 실패(FK 제약조건 등)가 발생하더라도,
          // 예외를 던지지 않고 무시하여 나머지 유저들의 삭제 배치가 중단되지 않도록합니다.
        }
      }
    }
  }
}
