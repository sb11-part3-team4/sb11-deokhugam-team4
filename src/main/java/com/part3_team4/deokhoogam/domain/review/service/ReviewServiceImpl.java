package com.part3_team4.deokhoogam.domain.review.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.part3_team4.deokhoogam.domain.review.exception.ReviewLikeAlreadyExistsException;
import com.part3_team4.deokhoogam.domain.book.entity.Book;
import com.part3_team4.deokhoogam.domain.book.exception.BookNotFoundException;
import com.part3_team4.deokhoogam.domain.book.repository.BookRepository;
import com.part3_team4.deokhoogam.domain.book.service.BookService;
import com.part3_team4.deokhoogam.domain.notification.service.NotificationService;
import com.part3_team4.deokhoogam.domain.review.dto.PopularReviewResponse;
import com.part3_team4.deokhoogam.domain.review.dto.ReviewCreateRequest;
import com.part3_team4.deokhoogam.domain.review.dto.ReviewLikeResponse;
import com.part3_team4.deokhoogam.domain.review.dto.ReviewListRequest;
import com.part3_team4.deokhoogam.domain.review.dto.ReviewResponse;
import com.part3_team4.deokhoogam.domain.review.dto.ReviewUpdateRequest;
import com.part3_team4.deokhoogam.domain.review.dto.ReviewWithLiked;
import com.part3_team4.deokhoogam.domain.review.entity.DeletedReview;
import com.part3_team4.deokhoogam.domain.review.entity.PopularReview;
import com.part3_team4.deokhoogam.domain.review.entity.Review;
import com.part3_team4.deokhoogam.domain.review.entity.ReviewDeletedEvent;
import com.part3_team4.deokhoogam.domain.review.entity.ReviewLike;
import com.part3_team4.deokhoogam.domain.review.exception.*;
import com.part3_team4.deokhoogam.domain.review.repository.DeletedReviewRepository;
import com.part3_team4.deokhoogam.domain.review.repository.PopularReviewRepository;
import com.part3_team4.deokhoogam.domain.review.repository.ReviewLikeRepository;
import com.part3_team4.deokhoogam.domain.review.repository.ReviewRepository;
import com.part3_team4.deokhoogam.domain.user.entity.User;
import com.part3_team4.deokhoogam.domain.user.exception.UserNotFoundException;
import com.part3_team4.deokhoogam.domain.user.repository.UserRepository;
import com.part3_team4.deokhoogam.global.common.PageResponse;
import com.part3_team4.deokhoogam.global.exception.ErrorKey;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

  private final ReviewRepository reviewRepository;
  private final BookRepository bookRepository;
  private final ReviewLikeRepository reviewLikeRepository;
  private final DeletedReviewRepository deletedReviewRepository;
  private final PopularReviewRepository popularReviewRepository;
  private final UserRepository userRepository;
  private final BookService bookService;
  private final NotificationService notificationService;
  private final ApplicationEventPublisher eventPublisher;
  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper redisObjectMapper;

  @Value("${cache.ranking.enabled:false}")    //캐싱 on/off(redis)
  private boolean cacheEnabled;


  @Override
  @Transactional
  public ReviewResponse createReview(UUID userId, ReviewCreateRequest request) {

    if (reviewRepository.existsByUserIdAndBookId(userId, request.bookId())) {
      throw ReviewAlreadyExistsException.withUserIdAndBookId(userId, request.bookId());
    }

    Book book = bookRepository.findById(request.bookId())
        .orElseThrow(() -> BookNotFoundException.withId(request.bookId()));

    User user = userRepository.findById(userId)
        .orElseThrow(() -> UserNotFoundException.withId(userId));

    Review review = Review.create(userId, request.bookId(), request.rating(), request.content());
    Review saved;
    try {
      saved = reviewRepository.save(review);
    } catch (DataIntegrityViolationException e) {
      throw ReviewAlreadyExistsException.withUserIdAndBookId(userId, request.bookId());
    }
    long reviewCount = reviewRepository.countByBookId(request.bookId());
    BigDecimal avgRating = reviewRepository.averageRatingByBookId(request.bookId())
        .setScale(2, RoundingMode.HALF_UP);
    bookService.updateReviewData(request.bookId(), Math.toIntExact(reviewCount), avgRating);

    log.info("[ReviewService] createReview 완료 - reviewId: {}", saved.getId());

    return new ReviewResponse(
        saved.getId(),
        saved.getUserId(),
        saved.getBookId(),
        saved.getRating(),
        saved.getContent(),
        book.getTitle(),
        book.getThumbnailUrl(),
        user.getName(),
        saved.getLikeCount(),
        saved.getCommentCount(),
        false,
        saved.getCreatedAt(),
        saved.getUpdatedAt()
    );
  }

  @Override
  @Transactional(readOnly = true)
  public ReviewResponse getReview(UUID reviewId, UUID userId) {

    ReviewWithLiked result = reviewRepository.findByIdWithLiked(reviewId, userId)
        .orElseThrow(() -> ReviewNotFoundException.withId(reviewId));

    Review review = result.review();

    Book book = bookRepository.findById(review.getBookId())
        .orElseThrow(() -> BookNotFoundException.withId(review.getBookId()));

    User user = userRepository.findById(review.getUserId())
        .orElseThrow(() -> UserNotFoundException.withId(review.getUserId()));
    boolean likedByMe = result.likedByMe();

    return new ReviewResponse(
        review.getId(),
        review.getUserId(),
        review.getBookId(),
        review.getRating(),
        review.getContent(),
        book.getTitle(),
        book.getThumbnailUrl(),
        user.getName(),
        review.getLikeCount(),
        review.getCommentCount(),
        likedByMe,
        review.getCreatedAt(),
        review.getUpdatedAt()
    );
  }

  @Override
  @Transactional
  public ReviewResponse updateReview(UUID reviewId, UUID userId, ReviewUpdateRequest request) {

    Review review = reviewRepository.findById(reviewId)
        .orElseThrow(() -> ReviewNotFoundException.withId(reviewId));

    if (!review.getUserId().equals(userId)) {
      throw ReviewNotOwnerException.withUserId(userId);
    }

    Book book = bookRepository.findById(review.getBookId())
        .orElseThrow(() -> BookNotFoundException.withId(review.getBookId()));

    User user = userRepository.findById(review.getUserId())
        .orElseThrow(() -> UserNotFoundException.withId(review.getUserId()));

    review.update(request.rating(), request.content());

    boolean likedByMe = reviewLikeRepository.existsByReviewIdAndUserId(reviewId, userId);

    log.info("[ReviewService] updateReview 완료 - reviewId: {}", reviewId);

    return new ReviewResponse(
        review.getId(),
        review.getUserId(),
        review.getBookId(),
        review.getRating(),
        review.getContent(),
        book.getTitle(),
        book.getThumbnailUrl(),
        user.getName(),
        review.getLikeCount(),
        review.getCommentCount(),
        likedByMe,
        review.getCreatedAt(),
        review.getUpdatedAt()
    );
  }

  @Override
  @Transactional
  public void deleteReview(UUID reviewId, UUID userId) {
    Review review = reviewRepository.findById(reviewId)
        .orElseThrow(() -> ReviewNotFoundException.withId(reviewId));
    if (!review.getUserId().equals(userId)) {
      throw ReviewNotOwnerException.withUserId(userId);
    }

    // 이벤트 발행 및 연관 데이터 선행 삭제 (EDA 적용)
    eventPublisher.publishEvent(new ReviewDeletedEvent(reviewId, false));
    popularReviewRepository.deleteAllByReviewId(reviewId);
    reviewLikeRepository.deleteAllByReviewId(reviewId);

    // 원본 데이터 백업 및 삭제
    deletedReviewRepository.save(DeletedReview.from(review));
    reviewRepository.delete(review);

    // 도서 통계 업데이트
    long reviewCount = reviewRepository.countByBookId(review.getBookId());
    BigDecimal avgRating = reviewRepository.averageRatingByBookId(review.getBookId())
        .setScale(2, RoundingMode.HALF_UP);
    bookService.updateReviewData(review.getBookId(), Math.toIntExact(reviewCount), avgRating);

    log.info("[ReviewService] deleteReview 완료 - reviewId: {}", reviewId);
  }

  @Override
  @Transactional
  public ReviewLikeResponse toggleLike(UUID reviewId, UUID userId) {
    Review review = reviewRepository.findById(reviewId)
        .orElseThrow(() -> ReviewNotFoundException.withId(reviewId));

    boolean alreadyLiked = reviewLikeRepository.existsByReviewIdAndUserId(reviewId, userId);

    if (alreadyLiked) {
      reviewLikeRepository.deleteByReviewIdAndUserId(reviewId, userId);
      reviewRepository.decrementLikeCount(reviewId); // DB 원자적 감소
    } else {
      User actor = userRepository.findById(userId)
          .orElseThrow(() -> UserNotFoundException.withId(userId));

      try {
        reviewLikeRepository.save(ReviewLike.create(reviewId, userId));
      } catch (DataIntegrityViolationException e) {
        if (e.getMessage() != null && e.getMessage().contains("idx_review_like_user_review_unique")) {
          throw ReviewLikeAlreadyExistsException.withReviewIdAndUserId(reviewId, userId);
        }
        throw e;
      }
      reviewRepository.incrementLikeCount(reviewId); // DB 원자적 증가

      notificationService.createLikeNotification(
          review.getUserId(), reviewId, review.getContent(), actor.getName(), userId);
    }
    int updatedCount = reviewRepository.getLikeCount(reviewId);

    log.info("[ReviewService] toggleLike 완료 - reviewId: {}, liked: {}", reviewId, !alreadyLiked);

    return new ReviewLikeResponse(reviewId, !alreadyLiked, updatedCount);
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<ReviewResponse> getReviews(UUID userId, ReviewListRequest request) {

    Sort sort = Sort.by(Sort.Direction.fromString(request.direction()), request.orderBy());
    Pageable pageable = PageRequest.of(0, request.limit() + 1, sort);

    List<Review> reviews;
    String cursor = request.cursor();
    String after = request.after();

    if (cursor != null && after != null) {
      UUID afterId = UUID.fromString(after);
      if ("rating".equals(request.orderBy())) {
        BigDecimal cursorRating = new BigDecimal(cursor);
        if ("DESC".equalsIgnoreCase(request.direction())) {
          reviews = reviewRepository.findReviewsWithCursorRatingDesc(
              request.userId(), request.bookId(), request.keyword(), cursorRating, afterId,
              pageable);
        } else {
          reviews = reviewRepository.findReviewsWithCursorRatingAsc(
              request.userId(), request.bookId(), request.keyword(), cursorRating, afterId,
              pageable);
        }
      } else {
        Instant cursorInstant = Instant.parse(cursor);
        if ("DESC".equalsIgnoreCase(request.direction())) {
          reviews = reviewRepository.findReviewsWithCursorCreatedAtDesc(
              request.userId(), request.bookId(), request.keyword(), cursorInstant, afterId,
              pageable);
        } else {
          reviews = reviewRepository.findReviewsWithCursorCreatedAtAsc(
              request.userId(), request.bookId(), request.keyword(), cursorInstant, afterId,
              pageable);
        }
      }
    } else {
      reviews = reviewRepository.findReviews(request.userId(), request.bookId(), request.keyword(),
          pageable);
    }

    boolean hasNext = reviews.size() > request.limit();
    if (hasNext) {
      reviews = reviews.subList(0, request.limit());
    }

    List<UUID> reviewIds = reviews.stream().map(Review::getId).toList();
    Set<UUID> likedReviewIds = new HashSet<>(
        reviewLikeRepository.findLikedReviewIds(userId, reviewIds));

    List<UUID> bookIds =
        reviews.stream().map(Review::getBookId).distinct().toList();
    List<UUID> userIds =
        reviews.stream().map(Review::getUserId).distinct().toList();
    Map<UUID, Book> bookMap = bookRepository.findAllById(bookIds).stream()
        .collect(Collectors.toMap(Book::getId, b -> b));
    Map<UUID, User> userMap = userRepository.findAllById(userIds).stream()
        .collect(Collectors.toMap(User::getId, u -> u));

    List<ReviewResponse> content = reviews.stream()
        .map(review -> {
          Book book = bookMap.get(review.getBookId());
          User user = userMap.get(review.getUserId());
          return new ReviewResponse(
              review.getId(),
              review.getUserId(),
              review.getBookId(),
              review.getRating(),
              review.getContent(),
              book != null ? book.getTitle() : null,
              book != null ? book.getThumbnailUrl() : null,
              user != null ? user.getName() : null,
              review.getLikeCount(),
              review.getCommentCount(),
              likedReviewIds.contains(review.getId()),
              review.getCreatedAt(),
              review.getUpdatedAt()
          );
        })
        .toList();

    String nextCursor = null;
    String nextAfter = null;
    if (hasNext && !content.isEmpty()) {
      ReviewResponse last = content.get(content.size() - 1);
      nextCursor = "rating".equals(request.orderBy())
          ? String.valueOf(last.rating())
          : last.createdAt() != null ? last.createdAt().toString() : null;
      nextAfter = last.id().toString();
    }

    return new PageResponse<>(content, nextCursor, nextAfter, content.size(), null, hasNext);
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<PopularReviewResponse> getPopularReviews(String period, String direction,
      String cursor, String after, int limit) {

    // 첫 페이지(cursor 없음)만 캐싱
    boolean cacheable = (cursor == null && cacheEnabled);
    String key = "ranking:review:" + period + ":" + direction + ":" + limit;

    if (cacheable) {
      try {
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
          log.debug("인기 리뷰 캐시 히트: key={}", key);
          return redisObjectMapper.readValue(
              cached, new TypeReference<PageResponse<PopularReviewResponse>>() {
              });
        }
        log.debug("인기 리뷰 캐시 미스: key={}", key);
      } catch (Exception e) {
        log.warn("인기 리뷰 캐시 읽기 실패, DB 폴백: key={}", key, e);
      }
    }

    Pageable pageable = PageRequest.of(0, limit + 1,
        Sort.by(Sort.Direction.fromString(direction), "rank"));
    List<PopularReview> popularReviews;
    if (cursor != null) {
      int cursorRank;
      try {
        cursorRank = Integer.parseInt(cursor);
      } catch (NumberFormatException e) {
        throw InvalidReviewException.withFieldAndValue(ErrorKey.WRONG_CURSOR, cursor,
            "cursor는 정수여야 합니다.");
      }
      if ("ASC".equalsIgnoreCase(direction)) {
        popularReviews = popularReviewRepository.findByPeriodAndRankGreaterThan(period, cursorRank,
            pageable);
      } else {
        popularReviews = popularReviewRepository.findByPeriodAndRankLessThan(period, cursorRank,
            pageable);
      }
    } else {
      if ("ASC".equalsIgnoreCase(direction)) {
        popularReviews = popularReviewRepository.findByPeriodOrderByRankAsc(period, pageable);
      } else {
        popularReviews = popularReviewRepository.findByPeriodOrderByRankDesc(period, pageable);
      }
    }

    boolean hasNext = popularReviews.size() > limit;
    if (hasNext) {
      popularReviews = popularReviews.subList(0, limit);
    }

    List<UUID> reviewIds =
        popularReviews.stream().map(PopularReview::getReviewId).toList();
    Map<UUID, Review> reviewMap = reviewRepository.findAllById(reviewIds)
        .stream().collect(Collectors.toMap(Review::getId, r -> r));
    List<UUID> bookIds =
        reviewMap.values().stream().map(Review::getBookId).distinct().toList();
    List<UUID> userIds =
        reviewMap.values().stream().map(Review::getUserId).distinct().toList();
    Map<UUID, Book> bookMap = bookRepository.findAllById(bookIds)
        .stream().collect(Collectors.toMap(Book::getId, b -> b));
    Map<UUID, User> userMap = userRepository.findAllById(userIds)
        .stream().collect(Collectors.toMap(User::getId, u -> u));

    List<PopularReviewResponse> content = popularReviews.stream()
        .map(pr -> {
          Review rev = reviewMap.get(pr.getReviewId());
          if (rev == null) {
            return null;
          }
          Book book = bookMap.get(rev.getBookId());
          User user = userMap.get(rev.getUserId());
          if (book == null || user == null) {
            return null;
          }
          return toPopularReviewResponse(pr, rev, book, user);
        })
        .filter(Objects::nonNull)
        .toList();

    String nextCursor = null;
    String nextAfter = null;
    if (hasNext && !content.isEmpty()) {
      PopularReviewResponse last = content.get(content.size() - 1);
      nextCursor = String.valueOf(last.rank());
      nextAfter = last.createdAt().toString();
    }

    PageResponse<PopularReviewResponse> result =
        new PageResponse<>(content, nextCursor, nextAfter, content.size(), null, hasNext);

    // 캐시 저장
    if (cacheable) {
      try {
        redisTemplate.opsForValue().set(
            key, redisObjectMapper.writeValueAsString(result), Duration.ofHours(2));
      } catch (Exception e) {
        log.warn("인기 리뷰 캐시 저장 실패: key={}", key, e);
      }
    }

    return result;
  }

  private PopularReviewResponse toPopularReviewResponse(PopularReview pr, Review review, Book book,
      User user) {
    return new PopularReviewResponse(
        pr.getId(), pr.getReviewId(), review.getBookId(),
        book.getTitle(), book.getThumbnailUrl(),
        review.getUserId(), user.getName(),
        review.getContent(), review.getRating(),
        pr.getPeriod(), pr.getCreatedAt(),
        pr.getRank(), pr.getScore(),
        review.getLikeCount(), review.getCommentCount()
    );
  }

  @Override
  @Transactional
  public void incrementCommentCount(UUID reviewId) {
    int updated = reviewRepository.incrementCommentCount(reviewId);
    if (updated == 0) {
      throw ReviewNotFoundException.withId(reviewId);
    }
    log.info("[ReviewService] incrementCommentCount 완료 - reviewId: {}", reviewId);
  }

  @Override
  @Transactional
  public void decrementCommentCount(UUID reviewId) {
    int updated = reviewRepository.decrementCommentCount(reviewId);
    if (updated == 0) {
      if (!reviewRepository.existsById(reviewId)) {
        throw ReviewNotFoundException.withId(reviewId);
      }
      return;
    }
    log.info("[ReviewService] decrementCommentCount 완료 - reviewId: {}", reviewId);
  }
}