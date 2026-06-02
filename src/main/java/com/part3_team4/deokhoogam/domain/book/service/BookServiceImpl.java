package com.part3_team4.deokhoogam.domain.book.service;

import com.part3_team4.deokhoogam.domain.book.dto.BookCreateRequest;
import com.part3_team4.deokhoogam.domain.book.dto.BookCursor;
import com.part3_team4.deokhoogam.domain.book.dto.BookDto;
import com.part3_team4.deokhoogam.domain.book.dto.BookGetListRequest;
import com.part3_team4.deokhoogam.domain.book.entity.Book;
import com.part3_team4.deokhoogam.domain.book.entity.SortType;
import com.part3_team4.deokhoogam.domain.book.exception.BookNotFoundException;
import com.part3_team4.deokhoogam.domain.book.exception.IsbnAlreadyExistsException;
import com.part3_team4.deokhoogam.domain.book.repository.BookRepository;
import com.part3_team4.deokhoogam.global.common.PageResponse;
import com.part3_team4.deokhoogam.global.exception.ErrorCode;
import com.part3_team4.deokhoogam.global.exception.InvalidRequestException;
import com.part3_team4.deokhoogam.global.util.CursorUtils;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

  private final BookRepository bookRepository;

  @Override
  @Transactional
  public BookDto create(BookCreateRequest request) {
    if (bookRepository.existsByIsbn(request.isbn())) {
      throw IsbnAlreadyExistsException.withIsbn(request.isbn());
    }

    // 동시성 문제 방지
    try {
      Book book = request.toEntity();
      return BookDto.from(bookRepository.save(book));
    } catch (DataIntegrityViolationException e) {
      throw IsbnAlreadyExistsException.withIsbn(request.isbn());
    }
  }

  @Override
  @Transactional(readOnly = true)
  public BookDto getDetails(UUID bookId) {

    Book book = bookRepository.findById(bookId)
        .orElseThrow(() -> BookNotFoundException.withId(bookId));

    return BookDto.from(book);

  }


  @Override
  public PageResponse<BookDto> getBooks(BookGetListRequest request) {

    if (request.limit() < 1 || request.limit() > 100) {
      throw new InvalidRequestException(ErrorCode.INVALID_INPUT_VALUE);
    }

    //커서 디코딩
    BookCursor cursor = CursorUtils.decodeCursor(request.cursor());
    //슬라이스 가져오기
    Slice<Book> books = bookRepository.getBooks(cursor, request);
    //Dto 변환
    List<BookDto> dtoList = books.stream().map(BookDto::from).toList();



    return PageResponse.<BookDto>builder()
        .content(dtoList)
        .hasNext(books.hasNext())
        .nextCursor(generateNextCursor(books,request.orderBy()))
        .build();
  }

  private String generateNextCursor(Slice<Book> books, SortType orderBy) {
    if (!books.hasNext() || books.getContent().isEmpty()) {
      return null; // 다음 페이지가 없으면 null 반환
    }

    // 현재 슬라이스의 마지막 데이터 추출
    Book lastBook = books.getContent().get(books.getContent().size() - 1);

    // 마지막 데이터의 값으로 다음 커서 객체 생성
    BookCursor newCursorObj = new BookCursor(
        orderBy,
        lastBook.getId(),
        lastBook.getCreatedAt()
    );

    return CursorUtils.encodeCursor(newCursorObj);
  }


}