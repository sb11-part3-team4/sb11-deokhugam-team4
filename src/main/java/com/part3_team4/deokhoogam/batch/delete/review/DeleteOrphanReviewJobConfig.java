package com.part3_team4.deokhoogam.batch.delete.review;

import com.part3_team4.deokhoogam.batch.listener.BatchJobMetricListener;
import com.part3_team4.deokhoogam.batch.listener.BatchStepMetricListener;
import com.part3_team4.deokhoogam.domain.review.entity.DeletedReview;
import com.part3_team4.deokhoogam.domain.review.repository.DeletedReviewRepository;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class DeleteOrphanReviewJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final DeletedReviewRepository deletedReviewRepository;

    private final BatchJobMetricListener batchJobMetricListener;
    private final BatchStepMetricListener batchStepMetricListener;

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
            .listener(batchJobMetricListener)
            .start(deleteOrphanReviewStep())
            .build();
    }

    @Bean
    public Step deleteOrphanReviewStep() {
        return new StepBuilder("deleteOrphanReviewStep", jobRepository)
            .<DeletedReview, DeletedReview>chunk(CHUNK_SIZE, transactionManager)
            .reader(orphanDeletedReviewReader())
            .writer(orphanDeletedReviewWriter())
            .listener(batchStepMetricListener)
            .build();
    }

    @Bean
    public JpaCursorItemReader<DeletedReview> orphanDeletedReviewReader() {
        return new JpaCursorItemReaderBuilder<DeletedReview>()
            .name("orphanDeletedReviewReader")
            .entityManagerFactory(entityManagerFactory)
            .queryString(ORPHAN_QUERY)
            .build();
    }

    @Bean
    public ItemWriter<DeletedReview> orphanDeletedReviewWriter() {
        return chunk -> deletedReviewRepository.deleteAllInBatch(List.copyOf(chunk.getItems()));
    }
}