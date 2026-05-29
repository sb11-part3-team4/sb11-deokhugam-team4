package com.part3_team4.deokhoogam.domain.notification.repository;

import com.part3_team4.deokhoogam.domain.notification.entity.Notification;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Notification Entity의 DB 접근을 담당하는 Repository입니다.
 *
 * JpaRepository를 상속하면 기본적인 CRUD 메서드를 사용할 수 있습니다.
 * 예:
 * - save
 * - findById
 * - findAll
 * - delete
 */
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

  /**
   * 특정 사용자의 미확인 알림 목록을 조회합니다.
   *
   * 전체 읽음 처리 API에서 사용합니다.
   *
   * 요구사항:
   * - 모든 알림을 한번에 확인할 수 있습니다.
   *
   * 구현 방식:
   * 1. confirmed = false인 알림만 조회
   * 2. Service에서 각 알림의 confirmed 값을 true로 변경
   *
   * Spring Data JPA가 메서드 이름을 해석해서 자동으로 쿼리를 만들어줍니다.
   */
  List<Notification> findAllByUserIdAndConfirmedFalse(UUID userId);
}