package com.part3_team4.deokhoogam.domain.user.service;

import com.part3_team4.deokhoogam.domain.user.entity.DeletedUser;
import com.part3_team4.deokhoogam.domain.user.entity.UserDeletedEvent;
import com.part3_team4.deokhoogam.domain.user.exception.BatchInfiniteLoopException;
import com.part3_team4.deokhoogam.domain.user.repository.DeletedUserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeletedUserAutoDeleteService {
  private final DeletedUserRepository deletedUserRepository;
  private final Clock clock;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public void deleteExpiredUsers() {
    long startTime = System.currentTimeMillis();

    Instant oneDayAgo = Instant.now(clock).minus(1, ChronoUnit.DAYS);

    log.info("유저 물리 삭제 배치 작업 시작. 기준 시각: {}", oneDayAgo);

    int pageSize = 100;
    long totalDeletedCount = 0;

    try {
      while (true) {
        Pageable pageable = PageRequest.of(0, pageSize);
        List<DeletedUser> oldUsers = deletedUserRepository.findByDeletedAtBefore(oneDayAgo, pageable);

        if (oldUsers.isEmpty()) {
          break;
        }

        long deletedInThisRound = 0;

        for (DeletedUser oldUser : oldUsers) {
          try {
            //삭제 전에 물리 이벤트를 먼저 발행
            eventPublisher.publishEvent(new UserDeletedEvent(oldUser.getId(), true));

            deletedUserRepository.delete(oldUser);
            totalDeletedCount++;
            deletedInThisRound++;
          } catch (Exception e) {
            log.warn("유저 삭제 중 예외 발생으로 항목 스킵. userId: {}, 사유: {}", oldUser.getId(), e.getMessage());
          }
        }

        if (deletedInThisRound == 0) {
          throw new BatchInfiniteLoopException();
        }
      }

      long duration = System.currentTimeMillis() - startTime;
      log.info("유저 물리 삭제 배치 작업 완료. 총 처리 건수: {}건, 소요시간: {}ms", totalDeletedCount, duration);

    } catch (Exception e) {
      log.error("유저 물리 삭제 배치 작업 실패", e);
      throw e;
    }
  }
}