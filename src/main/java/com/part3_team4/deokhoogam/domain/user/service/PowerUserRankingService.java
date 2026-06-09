package com.part3_team4.deokhoogam.domain.user.service;

import com.part3_team4.deokhoogam.domain.user.repository.PowerUserRankingRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PowerUserRankingService {

  private final PowerUserRankingRepository powerUserRankingRepository;

  public record UserScore(UUID userId, double score) {

  }

  public List<UserScore> calculateScores(Map<UUID, BigDecimal> reviewScores,
      Map<UUID, Long> likeCounts,
      Map<UUID, Long> commentCounts) {
    Set<UUID> activeUserIds = new HashSet<>();
    activeUserIds.addAll(reviewScores.keySet());
    activeUserIds.addAll(likeCounts.keySet());
    activeUserIds.addAll(commentCounts.keySet());

    List<UserScore> userScores = new ArrayList<>();

    for (UUID userId : activeUserIds) {
      double reviewScore = reviewScores.getOrDefault(userId, BigDecimal.ZERO).doubleValue();
      long likeCount = likeCounts.getOrDefault(userId, 0L);
      long commentCount = commentCounts.getOrDefault(userId, 0L);

      // (리뷰 * 0.5) + (좋아요 * 0.2) + (댓글 * 0.3)
      double totalScore = (reviewScore * 0.5) + (likeCount * 0.2) + (commentCount * 0.3);

      if (totalScore > 0) {
        userScores.add(new UserScore(userId, totalScore));
      }
    }
    return userScores;
  }
}
