package com.part3_team4.deokhoogam.domain.user.service;

import com.part3_team4.deokhoogam.domain.user.entity.UserDeletedEvent;
import com.part3_team4.deokhoogam.domain.user.repository.PowerUserRankingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PowerUserRankingEventListener {

  private final PowerUserRankingRepository powerUserRankingRepository;

  @EventListener
  @Transactional
  public void handleUserDeletedEvent(UserDeletedEvent event) {
    log.info("유저 삭제 이벤트 수신 (랭킹 파트) - userId: {}", event.userId());

    // 해당 유저의 랭킹 기록을 삭제하여 FK 제약조건 문제 해결
    powerUserRankingRepository.deleteByUserId(event.userId());

    log.info("유저 관련 랭킹 기록 삭제 완료 - userId: {}", event.userId());
  }
}