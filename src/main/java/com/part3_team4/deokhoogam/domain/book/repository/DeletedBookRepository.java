package com.part3_team4.deokhoogam.domain.book.repository;

import com.part3_team4.deokhoogam.domain.book.entity.DeletedBook;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeletedBookRepository extends JpaRepository<DeletedBook, UUID> {

}
