package com.part3_team4.deokhoogam.domain.notification.repository;

import com.part3_team4.deokhoogam.domain.notification.entity.Notification;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

  /**
   * 특정 사용자의 알림 첫 페이지를 최신순으로 조회합니다.
   *
   *  * @param userId 알림을 조회할 사용자 ID
   *  * @param pageable 조회 개수 제한 정보
   *  * @return 최신순으로 정렬된 알림 목록
   *
   * 사용처:
   * - GET /api/notifications 첫 페이지 조회
   *
   * 정렬 기준:
   * 1. createdAt DESC
   * 2. id DESC
   *
   * Pageable의 size를 limit + 1로 넘기면,
   * Service 계층에서 hasNext 여부를 판단할 수 있습니다.
   */
  List<Notification> findByUserIdOrderByCreatedAtDescIdDesc(UUID userId, Pageable pageable);

  /**
   * 특정 사용자의 알림 첫 페이지를 오래된 순으로 조회합니다.
   *
   * Swagger에서 direction=ASC도 허용하므로 ASC 조회도 준비합니다.
   */
  List<Notification> findByUserIdOrderByCreatedAtAscIdAsc(UUID userId, Pageable pageable);

  /**
   * 특정 사용자의 전체 알림 개수를 조회합니다.
   *
   * PageResponse의 totalElements 값을 만들 때 사용합니다.
   */
  long countByUserId(UUID userId);

  /**
   * 특정 사용자의 알림 다음 페이지를 최신순으로 조회합니다.
   *
   * 커서 기준:
   * - after: 이전 페이지 마지막 알림의 createdAt
   * - cursor: 이전 페이지 마지막 알림의 id
   *
   * 최신순 DESC 정렬에서 다음 페이지 조건:
   * - createdAt < after
   * - 또는 createdAt = after 이면서 id < cursor
   */
  @Query("""
      select n
      from Notification n
      where n.userId = :userId
        and (
          n.createdAt < :after
          or (n.createdAt = :after and n.id < :cursor)
        )
      order by n.createdAt desc, n.id desc
      """)

  /**
   * 커서 기준 다음 알림 페이지를 최신순으로 조회합니다.
   *
   * @param userId 알림을 조회할 사용자 ID
   * @param after 이전 페이지 마지막 알림의 생성 시각
   * @param cursor 이전 페이지 마지막 알림 ID
   * @param pageable 조회 개수 제한 정보
   * @return 커서 이후의 알림 목록
   */
  List<Notification> findNextPageDesc(
      UUID userId,
      Instant after,
      UUID cursor,
      Pageable pageable
  );

  /**
   * ASC 방향 커서 다음 페이지를 조회합니다.
   *
   * 이전 페이지 마지막 알림이 cursor/after일 때,
   * 그보다 더 나중에 생성된 알림들을 조회합니다.
   */
  @Query("""
      select n
      from Notification n
      where n.userId = :userId
        and (
          n.createdAt > :after
          or (n.createdAt = :after and n.id > :cursor)
        )
      order by n.createdAt asc, n.id asc
      """)
  List<Notification> findNextPageAsc(
      UUID userId,
      Instant after,
      UUID cursor,
      Pageable pageable
  );

}