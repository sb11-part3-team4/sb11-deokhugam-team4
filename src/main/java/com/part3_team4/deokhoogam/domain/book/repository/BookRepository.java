package com.part3_team4.deokhoogam.domain.book.repository;

import com.part3_team4.deokhoogam.domain.book.dto.BookCursor;
import com.part3_team4.deokhoogam.domain.book.dto.BookGetListRequest;
import com.part3_team4.deokhoogam.domain.book.entity.Book;
import java.util.UUID;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, UUID> {

  boolean existsByIsbn(String isbn);

  Slice<Book> getBooks(BookCursor cursor, BookGetListRequest request);



}
