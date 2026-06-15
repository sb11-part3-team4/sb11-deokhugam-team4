package com.part3_team4.deokhoogam.domain.book.repository;

import com.part3_team4.deokhoogam.domain.book.entity.OrphanThumbnail;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrphanThumbnailRepository extends JpaRepository<OrphanThumbnail, UUID> {

}