package com.part3_team4.deokhoogam.domain.user.repository;

import com.part3_team4.deokhoogam.domain.user.entity.DeleteUser;import java.util.UUID;import org.springframework.data.jpa.repository.JpaRepository;

public interface DeleteUserRepository extends JpaRepository<DeleteUser, UUID> {

}
