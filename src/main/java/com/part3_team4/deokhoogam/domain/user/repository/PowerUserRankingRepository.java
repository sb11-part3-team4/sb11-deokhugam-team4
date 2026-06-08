package com.part3_team4.deokhoogam.domain.user.repository;

import com.part3_team4.deokhoogam.domain.user.entity.PowerUserRanking;
import com.part3_team4.deokhoogam.domain.user.enums.PowerUserPeriod;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PowerUserRankingRepository extends JpaRepository<PowerUserRanking, UUID> {

  @Modifying(clearAutomatically = true)
  @Query("DELETE FROM PowerUserRanking p WHERE p.period = :period")
  void deleteByPeriod(@Param("period") PowerUserPeriod period);
}
