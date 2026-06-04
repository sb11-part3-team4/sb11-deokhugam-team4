package com.part3_team4.deokhoogam.domain.user.repository;


import com.part3_team4.deokhoogam.domain.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UserRepository extends JpaRepository<User, UUID> {
  boolean existsByName(String name);
  boolean existsByEmail(String email);
  Optional<User> findByEmail(String email);

  @Transactional
  @Modifying
  @Query(value = "DELETE FROM \"user\" WHERE id = :id", nativeQuery = true)
  void hardDeleteById(@Param("id") UUID id);
}
