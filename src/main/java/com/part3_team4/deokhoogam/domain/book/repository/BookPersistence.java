package com.part3_team4.deokhoogam.domain.book.repository;

import com.part3_team4.deokhoogam.domain.book.dto.BookUpdateRequest;
import com.part3_team4.deokhoogam.domain.book.entity.Book;
import com.part3_team4.deokhoogam.domain.book.exception.BookNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class BookPersistence {

  private final BookRepository bookRepository;

  @Transactional
  public Book save(Book book) {
    return bookRepository.save(book);
  }

  @Transactional
  public Book update(UUID id, BookUpdateRequest request, String newThumbnailUrl,
      String originalFilename) {
    Book book = bookRepository.findById(id)
        .orElseThrow(() -> BookNotFoundException.withId(id));

    book.updateBookInfo(
        request.title(),
        request.author(),
        request.description(),
        request.publisher(),
        request.publishedDate()
    );

    if (newThumbnailUrl != null) {
      book.updateThumbnail(newThumbnailUrl, originalFilename);
    }

    return book;
  }
}