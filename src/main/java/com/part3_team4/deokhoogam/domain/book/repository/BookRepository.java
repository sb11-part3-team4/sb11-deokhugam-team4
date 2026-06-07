package com.part3_team4.deokhoogam.domain.book.repository;

import com.part3_team4.deokhoogam.domain.book.entity.Book;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, UUID>, BookRepositoryCustom {

  boolean existsByIsbn(String isbn);





}
