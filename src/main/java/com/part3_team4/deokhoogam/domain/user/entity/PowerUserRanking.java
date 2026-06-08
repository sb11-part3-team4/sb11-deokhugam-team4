package com.part3_team4.deokhoogam.domain.user.entity;

import com.part3_team4.deokhoogam.domain.user.enums.PowerUserPeriod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "power_user_ranking")
public class PowerUserRanking {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PowerUserPeriod period;

  @Column(name = "score", nullable = false)
  private Double score;

  @Column(name = "ranking", nullable = false)
  private Integer ranking;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Builder
  public PowerUserRanking(UUID userId, PowerUserPeriod period, Double score, Integer ranking) {
    this.userId = userId;
    this.period = period;
    this.score = score;
    this.ranking = ranking;
  }
}
