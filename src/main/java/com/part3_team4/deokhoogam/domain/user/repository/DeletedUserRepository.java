package com.part3_team4.deokhoogam.domain.user.repository;

import com.part3_team4.deokhoogam.domain.user.entity.DeletedUser;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeletedUserRepository extends JpaRepository<DeletedUser, UUID> {
  boolean existsByEmail(String email);

  @Query("SELECT d.id FROM DeletedUser d WHERE d.deletedAt <= :cutoff")
  List<UUID> findUserIdsDeletedBefore(@Param("cutoff") Instant cutoff);

  List<DeletedUser> findByDeletedAtBefore(Instant deletedAt, Pageable pageable);
}
