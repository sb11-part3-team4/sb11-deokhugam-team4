package com.part3_team4.deokhoogam.domain.notification.repository;

import com.part3_team4.deokhoogam.domain.notification.entity.Notification;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

}
