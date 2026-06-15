package com.part3_team4.deokhoogam.domain.book.repository;

import static com.part3_team4.deokhoogam.domain.book.entity.QBook.book;

import com.part3_team4.deokhoogam.domain.book.dto.BookCursor;
import com.part3_team4.deokhoogam.domain.book.dto.BookGetListRequest;
import com.part3_team4.deokhoogam.domain.book.entity.Book;
import com.part3_team4.deokhoogam.domain.book.entity.Direction;
import com.part3_team4.deokhoogam.domain.book.entity.SortType;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;


@RequiredArgsConstructor
public class BookRepositoryImpl implements BookRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public Slice<Book> getBooks(BookCursor cursor, BookGetListRequest request) {

    // 1. QueryDSL로 데이터 조회
    List<Book> books = queryFactory
        .selectFrom(book)
        .where(
            // 검색어가 있다면 검색 조건 추가 (없으면 null 반환되어 무시됨)
            keywordContains(request.keyword()),
            // 커서 조건 추가
            cursorCondition(cursor, request.orderBy(), request.direction())
        )
        .orderBy(bookSort(request.orderBy(), request.direction())) // 정렬 조건
        .limit(request.limit() + 1) // 💡 프론트가 요청한 개수보다 1개 더 조회!
        .fetch();

    // 2. 조회된 결과를 Slice 객체로 변환하여 반환
    return checkLastPage(request.limit(), books);
  }


  // 커서 -> where 조건
  private BooleanExpression cursorCondition(BookCursor cursor, SortType sortType,
      Direction direction) {
    // 첫 페이지 요청이거나 데이터가 없으면 조건 패스
    if (cursor == null || cursor.getId() == null) {
      return null;
    }

    boolean isAsc = "ASC".equalsIgnoreCase(direction.toString());

    // orderBy 값에 따라 쿼리 분기 처리 (String -> 알맞은 타입으로 변환)
    return switch (sortType.getValue()) {

      // 1. 제목 기준
      case "title" -> {
        yield isAsc ? // 오름차순
            book.title.gt(cursor.getMainValue())
                .or(book.title.eq(cursor.getMainValue())
                    .and(book.createdAt.lt(cursor.getCreatedAt())))
                .or(
                    book.title.eq(cursor.getMainValue())
                        .and(book.createdAt.eq(cursor.getCreatedAt()))
                        .and(book.id.lt(cursor.getId())))
            //내림차순
            : book.title.lt(cursor.getMainValue()) // 제목
                //시간
                .or(book.title.eq(cursor.getMainValue())
                    .and(book.createdAt.lt(cursor.getCreatedAt())))
                .or(  //아이디
                    book.title.eq(cursor.getMainValue())
                        .and(book.createdAt.eq(cursor.getCreatedAt()))
                        .and(book.id.lt(cursor.getId())));


      }

      // 2. 평점 기준
      case "rating" -> {
        BigDecimal ratingVal = new BigDecimal(cursor.getMainValue());
        yield isAsc ?
            book.rating.gt(ratingVal)
                //시간
                .or(book.rating.eq(ratingVal).and(book.createdAt.lt(cursor.getCreatedAt())))
                .or(  //아이디
                    book.rating.eq(ratingVal)
                        .and(book.createdAt.eq(cursor.getCreatedAt()))
                        .and(book.id.lt(cursor.getId())))

            : book.rating.lt(ratingVal)
                //시간
                .or(book.rating.eq(ratingVal).and(book.createdAt.lt(cursor.getCreatedAt())))
                .or(  //아이디
                    book.rating.eq(ratingVal)
                        .and(book.createdAt.eq(cursor.getCreatedAt()))
                        .and(book.id.lt(cursor.getId())));
      }

      // 3. 리뷰 수 기준
      case "reviewCount" -> {
        Integer reviewVal = Integer.parseInt(cursor.getMainValue()); // String -> Integer 변환
        yield isAsc ?
            book.reviewCount.gt(reviewVal)
                //시간
                .or(book.reviewCount.eq(reviewVal).and(book.createdAt.lt(cursor.getCreatedAt())))
                .or(  //아이디
                    book.reviewCount.eq(reviewVal)
                        .and(book.createdAt.eq(cursor.getCreatedAt()))
                        .and(book.id.lt(cursor.getId())))

            :
                book.reviewCount.lt(reviewVal)
                    //시간
                    .or(book.reviewCount.eq(reviewVal)
                        .and(book.createdAt.lt(cursor.getCreatedAt())))
                    .or(  //아이디
                        book.reviewCount.eq(reviewVal)
                            .and(book.createdAt.eq(cursor.getCreatedAt()))
                            .and(book.id.lt(cursor.getId())));
      }

      //4. 출판일 기준
      case "publishedDate" -> {
        LocalDate dateVal = LocalDate.parse(cursor.getMainValue());
        yield isAsc ?
            book.publishedDate.gt(dateVal)
                //시간
                .or(book.publishedDate.eq(dateVal).and(book.createdAt.lt(cursor.getCreatedAt())))
                .or(  //아이디
                    book.publishedDate.eq(dateVal)
                        .and(book.createdAt.eq(cursor.getCreatedAt()))
                        .and(book.id.lt(cursor.getId())))

            :
                book.publishedDate.lt(dateVal)
                    //시간
                    .or(book.publishedDate.eq(dateVal)
                        .and(book.createdAt.lt(cursor.getCreatedAt())))
                    .or(  //아이디
                        book.publishedDate.eq(dateVal)
                            .and(book.createdAt.eq(cursor.getCreatedAt()))
                            .and(book.id.lt(cursor.getId())));
      }

      // 예외 처리
      default -> {
        throw new IllegalArgumentException("Invalid sort type: " + sortType);
      }
    };

  }

  private BooleanExpression keywordContains(String keyword) {
    if (keyword == null || keyword.isBlank()) {
      return null;
    }
    return Expressions.stringTemplate(
        "(COALESCE({0}, '') || ' ' || COALESCE({1}, '') || ' ' || COALESCE({2}, ''))",
        book.title, book.author, book.isbn
    ).contains(keyword);
  }

  //동적 정렬 조건
  private OrderSpecifier<?>[] bookSort(SortType orderBy, Direction direction) {
    boolean isAsc = "ASC".equalsIgnoreCase(direction.toString());

    return switch (orderBy.getValue()) {
      case "title" -> new OrderSpecifier[]{
          isAsc ? book.title.asc() : book.title.desc(), // 1차
          book.createdAt.desc(),                        // 2차
          book.id.desc()                                // 3차
      };
      case "rating" -> new OrderSpecifier[]{
          isAsc ? book.rating.asc() : book.rating.desc(),
          book.createdAt.desc(),
          book.id.desc()
      };
      case "reviewCount" -> new OrderSpecifier[]{
          isAsc ? book.reviewCount.asc() : book.reviewCount.desc(),
          book.createdAt.desc(),
          book.id.desc()
      };
      case "publishedDate" -> new OrderSpecifier[]{
          isAsc ? book.publishedDate.asc() : book.publishedDate.desc(),
          book.createdAt.desc(),
          book.id.desc()
      };

      default -> new OrderSpecifier[]{
          isAsc ? book.title.asc() : book.title.desc(), // 1차
          book.createdAt.desc(),                        // 2차
          book.id.desc()                                // 3차
      };

    };
  }


  private Slice<Book> checkLastPage(int limit, List<Book> results) {
    boolean hasNext = false;

    if (results.size() > limit) {
      hasNext = true;
      results.remove(limit);
    }

    return new SliceImpl<>(results, PageRequest.of(0, limit), hasNext);
  }


}
