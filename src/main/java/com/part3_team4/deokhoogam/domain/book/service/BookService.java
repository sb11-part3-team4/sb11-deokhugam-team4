package com.part3_team4.deokhoogam.domain.book.service;

import com.part3_team4.deokhoogam.domain.book.dto.BookCreateRequest;
import com.part3_team4.deokhoogam.domain.book.dto.BookDto;
import com.part3_team4.deokhoogam.domain.book.dto.BookGetListRequest;
import com.part3_team4.deokhoogam.domain.book.dto.BookUpdateRequest;
import com.part3_team4.deokhoogam.domain.book.dto.NaverBookDto;
import com.part3_team4.deokhoogam.global.common.PageResponse;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;


public interface BookService {

  BookDto create(BookCreateRequest request, MultipartFile thumbnailFile);

  BookDto update(UUID id, BookUpdateRequest request, MultipartFile thumbnailFile);

  void updateReviewData(UUID bookId, int reviewCount, BigDecimal rating);

  BookDto getDetails(UUID bookId);

  NaverBookDto getByIsbn(String isbn);

  PageResponse<BookDto> getBooks(BookGetListRequest request);

  void delete(UUID bookId);

  void deleteHard(UUID bookId);


}