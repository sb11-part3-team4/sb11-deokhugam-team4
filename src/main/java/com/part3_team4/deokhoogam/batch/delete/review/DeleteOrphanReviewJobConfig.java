package com.part3_team4.deokhoogam.batch.delete.review;

import com.part3_team4.deokhoogam.domain.review.entity.DeletedReview;
import com.part3_team4.deokhoogam.domain.review.repository.DeletedReviewRepository;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;

@Configuration
@RequiredArgsConstructor
public class DeleteOrphanReviewJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final DeletedReviewRepository deletedReviewRepository;

    private static final int CHUNK_SIZE = 100;

    private static final String ORPHAN_QUERY =
        "SELECT dr FROM DeletedReview dr " +
        "WHERE (NOT EXISTS (SELECT 1 FROM Book b WHERE b.id = dr.bookId) " +
        "  AND NOT EXISTS (SELECT 1 FROM DeletedBook db WHERE db.id = dr.bookId)) " +
        "  OR (NOT EXISTS (SELECT 1 FROM User u WHERE u.id = dr.userId) " +
        "  AND NOT EXISTS (SELECT 1 FROM DeletedUser du WHERE du.id = dr.userId))";

    @Bean
    public Job deleteOrphanReviewJob() {
        return new JobBuilder("deleteOrphanReviewJob", jobRepository)
            .start(deleteOrphanReviewStep())
            .build();
    }

    @Bean
    public Step deleteOrphanReviewStep() {
        return new StepBuilder("deleteOrphanReviewStep", jobRepository)
            .<DeletedReview, DeletedReview>chunk(CHUNK_SIZE, transactionManager)
            .reader(orphanDeletedReviewReader())
            .writer(orphanDeletedReviewWriter())
            .build();
    }

    @Bean
    public JpaPagingItemReader<DeletedReview> orphanDeletedReviewReader() {
        return new JpaPagingItemReaderBuilder<DeletedReview>()
            .name("orphanDeletedReviewReader")
            .entityManagerFactory(entityManagerFactory)
            .pageSize(CHUNK_SIZE)
            .queryString(ORPHAN_QUERY)
            .saveState(false)
            .build();
    }

    @Bean
    public ItemWriter<DeletedReview> orphanDeletedReviewWriter() {
        return chunk -> deletedReviewRepository.deleteAllInBatch(new ArrayList<>(chunk.getItems()));
    }
}