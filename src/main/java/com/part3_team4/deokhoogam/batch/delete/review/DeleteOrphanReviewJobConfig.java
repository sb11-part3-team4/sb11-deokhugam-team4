package com.part3_team4.deokhoogam.batch.delete.review;

import com.part3_team4.deokhoogam.domain.review.entity.DeletedReview;
import com.part3_team4.deokhoogam.domain.review.repository.DeletedReviewRepository;
import jakarta.persistence.EntityManagerFactory;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
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
        "  AND NOT EXISTS (SELECT 1 FROM DeletedUser du WHERE du.id = dr.userId))" +
        "  OR dr.deletedAt <= :cutoff";

    @Bean
    public Job deleteOrphanReviewJob() {
        return new JobBuilder("deleteOrphanReviewJob", jobRepository)
            .listener(new JobExecutionListener() {
                @Override
                public void beforeJob(JobExecution jobExecution) {
                    log.info("배치 시작 - job: {}", jobExecution.getJobInstance().getJobName());
                }

                @Override
                public void afterJob(JobExecution jobExecution) {
                    String jobName = jobExecution.getJobInstance().getJobName();
                    long writeCount = jobExecution.getStepExecutions().stream()
                        .mapToLong(StepExecution::getWriteCount).sum();
                    long durationMs = (jobExecution.getStartTime() != null && jobExecution.getEndTime() != null)
                        ? Duration.between(jobExecution.getStartTime(), jobExecution.getEndTime()).toMillis()
                        : -1L;
                    if (jobExecution.getStatus() == BatchStatus.FAILED) {
                        jobExecution.getAllFailureExceptions()
                            .forEach(e -> log.error("배치 실패 - job: {}", jobName, e));
                    } else {
                        log.info("배치 완료 - job: {}, 처리 건수: {}, 소요시간: {}ms", jobName, writeCount, durationMs);
                    }
                }
            })
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
    @StepScope
    public JpaCursorItemReader<DeletedReview> orphanDeletedReviewReader() {
        return new JpaCursorItemReaderBuilder<DeletedReview>()
            .name("orphanDeletedReviewReader")
            .entityManagerFactory(entityManagerFactory)
            .queryString(ORPHAN_QUERY)
            .parameterValues(Map.of("cutoff", Instant.now().minus(30, ChronoUnit.DAYS)))
            .build();
    }

    @Bean
    public ItemWriter<DeletedReview> orphanDeletedReviewWriter() {
        return chunk -> deletedReviewRepository.deleteAllInBatch(List.copyOf(chunk.getItems()));
    }
}