package com.part3_team4.deokhoogam.batch.delete.comment;

import com.part3_team4.deokhoogam.domain.comment.entity.DeletedComment;
import com.part3_team4.deokhoogam.domain.comment.repository.DeletedCommentRepository;
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
public class DeleteOrphanCommentJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final DeletedCommentRepository deletedCommentRepository;

    private static final int CHUNK_SIZE = 100;

    private static final String ORPHAN_QUERY =
        "SELECT dc FROM DeletedComment dc " +
        "WHERE (dc.reviewId NOT IN (SELECT r.id FROM Review r) " +
        "  AND dc.reviewId NOT IN (SELECT dr.id FROM DeletedReview dr)) " +
        "  OR (dc.userId NOT IN (SELECT u.id FROM User u) " +
        "  AND dc.userId NOT IN (SELECT du.id FROM DeletedUser du))";

    @Bean
    public Job deleteOrphanCommentJob() {
        return new JobBuilder("deleteOrphanCommentJob", jobRepository)
            .start(deleteOrphanCommentStep())
            .build();
    }

    @Bean
    public Step deleteOrphanCommentStep() {
        return new StepBuilder("deleteOrphanCommentStep", jobRepository)
            .<DeletedComment, DeletedComment>chunk(CHUNK_SIZE, transactionManager)
            .reader(orphanDeletedCommentReader())
            .writer(orphanDeletedCommentWriter())
            .build();
    }

    @Bean
    public JpaPagingItemReader<DeletedComment> orphanDeletedCommentReader() {
        return new JpaPagingItemReaderBuilder<DeletedComment>()
            .name("orphanDeletedCommentReader")
            .entityManagerFactory(entityManagerFactory)
            .pageSize(CHUNK_SIZE)
            .queryString(ORPHAN_QUERY)
            .saveState(false)
            .build();
    }

    @Bean
    public ItemWriter<DeletedComment> orphanDeletedCommentWriter() {
        return chunk -> deletedCommentRepository.deleteAllInBatch(new ArrayList<>(chunk.getItems()));
    }
}