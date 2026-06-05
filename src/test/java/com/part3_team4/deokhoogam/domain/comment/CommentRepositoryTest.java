package com.part3_team4.deokhoogam.domain.comment;

import static org.assertj.core.api.Assertions.assertThat;

import com.part3_team4.deokhoogam.domain.comment.entity.Comment;
import com.part3_team4.deokhoogam.domain.comment.repository.CommentRepository;
import com.part3_team4.deokhoogam.domain.review.entity.Review;
import com.part3_team4.deokhoogam.domain.user.entity.User;
import com.part3_team4.deokhoogam.global.config.JpaAuditingConfig;
import com.part3_team4.deokhoogam.global.config.QuerydslConfig;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@Import({JpaAuditingConfig.class, QuerydslConfig.class})
@ActiveProfiles("test")
class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private TestEntityManager em;

    private User testUser;
    private Review testReview;

    @BeforeEach
    void setUp() {
        testUser = new User("test@email.com", "테스트유저", "password");
        em.persist(testUser);
        testReview = Review.create(testUser.getId(), UUID.randomUUID(), 5, "테스트 리뷰입니다.");
        em.persist(testReview);
        em.flush();
    }

    /**
     * @CreatedDate 는 @PrePersist 에서 항상 현재 시각으로 덮어쓴다.
     * save() 전 ReflectionTestUtils.setField() 는 무시되므로,
     * flush()로 INSERT 확정 → native SQL UPDATE(CAST로 UUID 바인딩) → em.clear() 후 재조회하여
     * DB 실제 값을 반환한다. 커서 비교도 이 반환값을 사용해야 정확하다.
     */
    private Comment saveWithCreatedAt(String content, Instant createdAt) {
        Comment comment = Comment.create(testReview, testUser, content);
        commentRepository.save(comment);
        em.flush();
        em.getEntityManager()
                .createNativeQuery("UPDATE comment SET created_at = :t WHERE id = CAST(:id AS UUID)")
                .setParameter("t", createdAt)
                .setParameter("id", comment.getId().toString())
                .executeUpdate();
        em.clear();
        return commentRepository.findById(comment.getId()).get();
    }

    @Test
    @DisplayName("댓글을 저장하고 ID로 조회한다")
    void saveAndFindById() {
        Comment comment = Comment.create(testReview, testUser, "테스트 댓글입니다.");
        commentRepository.save(comment);
        em.flush();
        em.clear();

        Optional<Comment> result = commentRepository.findById(comment.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getContent()).isEqualTo("테스트 댓글입니다.");
        assertThat(result.get().getReviewId()).isEqualTo(testReview.getId());
        assertThat(result.get().getUserId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("review_id로 댓글 목록을 시간 역순(createdAt DESC) 조회한다")
    void findByReviewId_orderedByCreatedAt() {
        Instant base = Instant.now();
        saveWithCreatedAt("첫 번째 댓글", base.minusSeconds(20));
        saveWithCreatedAt("두 번째 댓글", base.minusSeconds(10));
        saveWithCreatedAt("세 번째 댓글", base);

        List<Comment> result = commentRepository.findByReviewIdOrderByCreatedAtDesc(
                testReview.getId(), PageRequest.of(0, 10));

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getContent()).isEqualTo("세 번째 댓글");
        assertThat(result.get(1).getContent()).isEqualTo("두 번째 댓글");
        assertThat(result.get(2).getContent()).isEqualTo("첫 번째 댓글");
    }

    @Test
    @DisplayName("커서 이전의 댓글만 조회된다 (createdAt 기준)")
    void findBeforeCursor_returnsOnlyEarlierComments() {
        Instant base = Instant.now();
        saveWithCreatedAt("첫 번째 댓글", base.minusSeconds(20));
        saveWithCreatedAt("두 번째 댓글", base.minusSeconds(10));
        Comment third = saveWithCreatedAt("세 번째 댓글", base);

        Instant cursor = third.getCreatedAt();

        List<Comment> result = commentRepository.findByReviewIdAndCreatedAtBeforeOrderByCreatedAtDesc(
                testReview.getId(), cursor, PageRequest.of(0, 10));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getContent()).isEqualTo("두 번째 댓글");
        assertThat(result.get(1).getContent()).isEqualTo("첫 번째 댓글");
    }

    // TODO: 동일 createdAt + 서로 다른 id 시나리오 회귀 테스트 추가
    //       현재 커서 테스트는 타임스탬프가 모두 달라 동일 createdAt 다건 시 페이지 누락/중복을 검증하지 못함
    //       커서를 (createdAt, id) 복합키로 고도화한 뒤 아래 케이스를 작성할 것:
    //       - 같은 createdAt을 가진 댓글 N개 중 절반을 1페이지로 조회했을 때 2페이지에서 나머지가 누락/중복 없이 반환되는지 검증

    @Test
    @DisplayName("다른 reviewId의 댓글은 조회되지 않는다")
    void findByReviewId_excludesOtherReviews() {
        Review otherReview = Review.create(testUser.getId(), UUID.randomUUID(), 3, "다른 리뷰입니다.");
        em.persist(otherReview);
        em.flush();

        commentRepository.save(Comment.create(testReview, testUser, "리뷰A 댓글"));
        commentRepository.save(Comment.create(otherReview, testUser, "리뷰B 댓글"));
        em.flush();
        em.clear();

        List<Comment> result = commentRepository.findByReviewIdOrderByCreatedAtDesc(
                testReview.getId(), PageRequest.of(0, 10));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("리뷰A 댓글");
    }

    @Test
    @DisplayName("Pageable size만큼만 댓글이 반환된다 (5개 중 2개만 조회)")
    void findByReviewId_respectsPageableLimit() {
        Instant base = Instant.now();
        for (int i = 1; i <= 5; i++) {
            saveWithCreatedAt(i + "번째 댓글", base.minusSeconds(10 - i));
        }

        // DESC 정렬이므로 반환되는 2개는 가장 최신(5번째, 4번째) 댓글이다
        List<Comment> result = commentRepository.findByReviewIdOrderByCreatedAtDesc(
                testReview.getId(), PageRequest.of(0, 2));

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("커서가 가장 오래된 댓글의 createdAt이면 빈 리스트를 반환한다")
    void findBeforeCursor_withFirstCursor_returnsEmpty() {
        Instant base = Instant.now();
        Comment first = saveWithCreatedAt("첫 번째 댓글", base.minusSeconds(20));
        saveWithCreatedAt("두 번째 댓글", base.minusSeconds(10));
        saveWithCreatedAt("세 번째 댓글", base);

        Instant cursor = first.getCreatedAt();

        List<Comment> result = commentRepository.findByReviewIdAndCreatedAtBeforeOrderByCreatedAtDesc(
                testReview.getId(), cursor, PageRequest.of(0, 10));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("해당 reviewId의 댓글이 모두 삭제된다")
    void deleteByReviewId_removesAllMatchingComments() {
        Review otherReview = Review.create(testUser.getId(), UUID.randomUUID(), 3, "다른 리뷰입니다.");
        em.persist(otherReview);
        em.flush();

        commentRepository.save(Comment.create(testReview, testUser, "리뷰A 댓글 1"));
        commentRepository.save(Comment.create(testReview, testUser, "리뷰A 댓글 2"));
        commentRepository.save(Comment.create(otherReview, testUser, "리뷰B 댓글"));
        em.flush();
        em.clear();

        commentRepository.deleteByReviewId(testReview.getId());
        em.flush();
        em.clear();

        assertThat(commentRepository.countByReviewId(testReview.getId())).isZero();
        assertThat(commentRepository.countByReviewId(otherReview.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("해당 reviewId의 댓글 수를 정확히 반환한다")
    void countByReviewId_returnsAccurateCount() {
        Review otherReview = Review.create(testUser.getId(), UUID.randomUUID(), 3, "다른 리뷰입니다.");
        em.persist(otherReview);
        em.flush();

        commentRepository.save(Comment.create(testReview, testUser, "리뷰A 댓글 1"));
        commentRepository.save(Comment.create(testReview, testUser, "리뷰A 댓글 2"));
        commentRepository.save(Comment.create(otherReview, testUser, "리뷰B 댓글"));
        em.flush();
        em.clear();

        assertThat(commentRepository.countByReviewId(testReview.getId())).isEqualTo(2);
        assertThat(commentRepository.countByReviewId(otherReview.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("해당 userId의 살아있는 댓글 수를 정확히 반환한다")
    void countByUserId_returnsAccurateCount() {
        User otherUser = new User("other@email.com", "다른유저", "password");
        em.persist(otherUser);
        em.flush();

        commentRepository.save(Comment.create(testReview, testUser, "유저A 댓글 1"));
        commentRepository.save(Comment.create(testReview, testUser, "유저A 댓글 2"));
        commentRepository.save(Comment.create(testReview, otherUser, "유저B 댓글"));
        em.flush();
        em.clear();

        assertThat(commentRepository.countByUserId(testUser.getId())).isEqualTo(2);
        assertThat(commentRepository.countByUserId(otherUser.getId())).isEqualTo(1);
    }
}