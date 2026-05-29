package com.part3_team4.deokhoogam.domain.notification.entity;

import com.part3_team4.deokhoogam.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림 Entity입니다.
 *
 * 알림은 특정 사용자(userId)에게 전달되며,
 * 특정 리뷰(reviewId)에 대한 이벤트를 나타냅니다.
 *
 * 현재 구조에서는 User, Review Entity와 직접 연관관계를 맺지 않고
 * UUID만 저장합니다.
 *
 * 이렇게 하면 알림 도메인이 다른 도메인의 Entity 구조에 덜 의존하게 되고,
 * 지금처럼 notification 패키지만 수정할 수 있는 상황에서도 구현하기 쉽습니다.
 */
@Entity
@Table(
    name = "notifications",
    indexes = {
        /**
         * 알림 목록 조회용 인덱스입니다.
         *
         * 요구사항:
         * - 사용자별 알림 조회
         * - 최근 시간 순 정렬
         * - 커서 페이지네이션
         *
         * 따라서 user_id, created_at, id 조합으로 인덱스를 둡니다.
         */
        @Index(name = "idx_notification_user_pagination", columnList = "user_id, created_at DESC, id DESC"),

        /**
         * 오래된 확인 알림 삭제용 인덱스입니다.
         *
         * 요구사항:
         * - 확인한 알림 중 1주일 경과된 알림 자동 삭제
         *
         * 삭제 조건에서 confirmed와 updated_at을 함께 사용할 예정입니다.
         */
        @Index(name = "idx_notification_batch_clean", columnList = "confirmed, updated_at")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {

  /**
   * 알림을 받을 사용자 ID입니다.
   *
   * 예:
   * 내가 쓴 리뷰에 다른 사용자가 댓글을 달면,
   * userId는 "리뷰 작성자 ID"입니다.
   */
  @Column(name = "user_id", nullable = false, columnDefinition = "UUID")
  private UUID userId;

  /**
   * 알림이 연결된 리뷰 ID입니다.
   *
   * 알림을 클릭했을 때 해당 리뷰 상세로 이동하는 데 사용할 수 있습니다.
   */
  @Column(name = "review_id", nullable = false, columnDefinition = "UUID")
  private UUID reviewId;

  /**
   * 알림에 표시할 리뷰 내용입니다.
   *
   * Swagger 응답에 reviewContent가 포함되어 있으므로 저장합니다.
   * 리뷰가 나중에 삭제되거나 수정되어도 알림 당시의 내용을 보여줄 수 있다는 장점이 있습니다.
   */
  @Column(name = "review_content", nullable = false, columnDefinition = "TEXT")
  private String reviewContent;

  /**
   * 사용자에게 보여줄 알림 메시지입니다.
   *
   * 예:
   * - "우디님이 내 리뷰에 댓글을 남겼습니다."
   * - "버즈님이 내 리뷰에 좋아요를 눌렀습니다."
   */
  @Column(name = "message", nullable = false, length = 512)
  private String message;

  /**
   * 알림 확인 여부입니다.
   *
   * 새로 생성된 알림은 아직 사용자가 확인하지 않았으므로 false가 기본값입니다.
   */
  @Builder.Default
  @Column(name = "confirmed", nullable = false)
  private boolean confirmed = false;

  /**
   * 알림 확인 상태를 변경하는 도메인 메서드입니다.
   *
   * 단순히 setter를 열어두는 대신,
   * 알림 상태 변경이라는 의미가 드러나는 메서드를 제공합니다.
   *
   * Swagger 요청은 confirmed 값을 true/false로 받을 수 있으므로,
   * 기존 markAsRead()보다 boolean 값을 받는 updateConfirmed가 더 유연합니다.
   */
  public void updateConfirmed(boolean confirmed) {
    this.confirmed = confirmed;
  }
}