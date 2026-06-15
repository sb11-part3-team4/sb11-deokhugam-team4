package com.part3_team4.deokhoogam.batch.BookRanking;


import com.part3_team4.deokhoogam.domain.book.dto.ranking.BookScoreProjection;
import com.part3_team4.deokhoogam.domain.book.entity.Book;
import com.part3_team4.deokhoogam.domain.book.entity.BookRanking;
import com.part3_team4.deokhoogam.domain.book.entity.PeriodType;
import com.part3_team4.deokhoogam.domain.book.repository.BookRepository;
import com.part3_team4.deokhoogam.domain.book.repository.ranking.BookRankingRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingCalculator {

  private final BookRankingRepository bookRankingRepository;
  private final BookRepository bookRepository;
  private final StringRedisTemplate redisTemplate;


  private final RankingScoreCalculator scoreCalculator;

  @Transactional
  public void calculateAndSave(PeriodType period) {

    // 1. 기간 계산
    RankingPeriod range = RankingPeriod.of(period);

    // 2. 재료 세기 (책별 리뷰수, 평점)
    List<BookScoreProjection> scores =
        bookRankingRepository.aggregateScores(range.getStart(), range.getEnd());

    // 3. 점수 계산 → 임시 객체로 모음
    List<Scored> scored = new ArrayList<>();
    for (BookScoreProjection s : scores) {
      BigDecimal score = scoreCalculator.calculate(s.getReviewCount(), s.getAvgRating());
      scored.add(new Scored(s, score));
    }

    // 4. 동점 정렬용: 책 createdAt 한 번에 조회
    List<UUID> bookIds = scored.stream().map(s -> s.projection().getBookId()).toList();
    Map<UUID, Instant> createdAtMap = bookRepository.findAllById(bookIds).stream()
        .collect(Collectors.toMap(Book::getId, Book::getCreatedAt));

    // 점수 내림차순, 동점이면 createdAt 오름차순(먼저 등록된 책 우선)
    scored.sort(
        Comparator.comparing(Scored::score).reversed()
            .thenComparing(s -> createdAtMap.getOrDefault(s.projection().getBookId(), Instant.MAX))
    );

    List<BookRanking> rankings = new ArrayList<>();
    int rank = 1;
    for (Scored s : scored) {
      rankings.add(BookRanking.builder()
          .bookId(s.projection().getBookId())
          .period(period)
          .score(s.score())
          .ranking(rank++)
          .reviewCount(s.projection().getReviewCount())
          .rating(s.projection().getAvgRating())
          .build());
    }

    // 5. 기존 기간 데이터 삭제 후 새로 저장 (멱등성)
    bookRankingRepository.deleteByPeriod(period);
    bookRankingRepository.flush();
    bookRankingRepository.saveAll(rankings);

    // 커밋 후 캐시 삭제
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              try {
                String pattern = "ranking:book:" + period + ":*";
                ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
                List<String> keysToDelete = new ArrayList<>();
                try (Cursor<String> cursor = redisTemplate.scan(options)) {
                  cursor.forEachRemaining(keysToDelete::add);
                }
                if (!keysToDelete.isEmpty()) {
                  redisTemplate.delete(keysToDelete);
                }
              } catch (Exception e) {
                log.warn("Redis 캐시 삭제 실패 (도서): period={}", period, e);
              }
            }
          }
      );
    }

    log.info("기간별 랭킹 산출 완료: period={}, 대상도서={}건", period, rankings.size());
  }

  // 점수 계산 중간 결과를 잠깐 담는 용도
  private record Scored(BookScoreProjection projection, BigDecimal score) {

  }
}

