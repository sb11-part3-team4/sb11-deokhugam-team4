package com.part3_team4.deokhoogam.domain.user.repository;


import com.part3_team4.deokhoogam.domain.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
  boolean existsByName(String name);
  boolean existsByEmail(String email);
  Optional<User> findByEmail(String email);
}
