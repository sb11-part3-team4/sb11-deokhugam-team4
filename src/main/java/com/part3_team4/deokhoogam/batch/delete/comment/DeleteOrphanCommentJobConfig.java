package com.part3_team4.deokhoogam.batch.delete.comment;

import com.part3_team4.deokhoogam.batch.listener.BatchJobMetricListener;
import com.part3_team4.deokhoogam.batch.listener.BatchStepMetricListener;
import com.part3_team4.deokhoogam.domain.comment.entity.DeletedComment;
import com.part3_team4.deokhoogam.domain.comment.repository.DeletedCommentRepository;
import jakarta.persistence.EntityManagerFactory;
import java.time.Duration;
import java.util.List;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
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
public class DeleteOrphanCommentJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final DeletedCommentRepository deletedCommentRepository;

    private final BatchJobMetricListener batchJobMetricListener;
    private final BatchStepMetricListener batchStepMetricListener;

    private static final int CHUNK_SIZE = 100;

    private static final String ORPHAN_QUERY =
        "SELECT dc FROM DeletedComment dc " +
        "WHERE (NOT EXISTS (SELECT 1 FROM Review r WHERE r.id = dc.reviewId) " +
        "  AND NOT EXISTS (SELECT 1 FROM DeletedReview dr WHERE dr.id = dc.reviewId)) " +
        "  OR (NOT EXISTS (SELECT 1 FROM User u WHERE u.id = dc.userId) " +
        "  AND NOT EXISTS (SELECT 1 FROM DeletedUser du WHERE du.id = dc.userId))";

    @Bean
    public Job deleteOrphanCommentJob() {
        return new JobBuilder("deleteOrphanCommentJob", jobRepository)
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
            .start(deleteOrphanCommentStep())
            .build();
    }

    @Bean
    public Step deleteOrphanCommentStep() {
        return new StepBuilder("deleteOrphanCommentStep", jobRepository)
            .<DeletedComment, DeletedComment>chunk(CHUNK_SIZE, transactionManager)
            .reader(orphanDeletedCommentReader())
            .writer(orphanDeletedCommentWriter())
            .listener(batchStepMetricListener)
            .build();
    }

    @Bean
    public JpaCursorItemReader<DeletedComment> orphanDeletedCommentReader() {
        return new JpaCursorItemReaderBuilder<DeletedComment>()
            .name("orphanDeletedCommentReader")
            .entityManagerFactory(entityManagerFactory)
            .queryString(ORPHAN_QUERY)
            .build();
    }

    @Bean
    public ItemWriter<DeletedComment> orphanDeletedCommentWriter() {
        return chunk -> deletedCommentRepository.deleteAllInBatch(List.copyOf(chunk.getItems()));
    }
}