package com.part3_team4.deokhoogam.domain.user.repository;

import com.part3_team4.deokhoogam.domain.user.entity.PowerUserRanking;
import com.part3_team4.deokhoogam.domain.user.enums.PowerUserPeriod;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface PowerUserRankingRepository extends JpaRepository<PowerUserRanking, UUID>,
    PowerUserQueryRepository {

  List<PowerUserRanking> findByPeriodOrderByRankingAsc(PowerUserPeriod period);

  @Modifying(clearAutomatically = true)
  @Query("DELETE FROM PowerUserRanking p WHERE p.period = :period")
  void deleteByPeriod(@Param("period") PowerUserPeriod period);

  // 유저 삭제 이벤트용 삭제 메서드 추가
  @Modifying
  @Transactional
  void deleteByUserId(UUID userId);
}
