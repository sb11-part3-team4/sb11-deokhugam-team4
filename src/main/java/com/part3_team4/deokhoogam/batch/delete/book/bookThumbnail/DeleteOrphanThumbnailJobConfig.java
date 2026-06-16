package com.part3_team4.deokhoogam.batch.delete.book.bookThumbnail;

import com.part3_team4.deokhoogam.batch.listener.BatchJobMetricListener;
import com.part3_team4.deokhoogam.batch.listener.BatchStepMetricListener;
import com.part3_team4.deokhoogam.batch.listener.JobLoggingListener;
import com.part3_team4.deokhoogam.batch.listener.SkipLoggingListener;
import com.part3_team4.deokhoogam.domain.book.entity.OrphanThumbnail;
import com.part3_team4.deokhoogam.domain.book.repository.OrphanThumbnailRepository;
import com.part3_team4.deokhoogam.global.exception.storage.StorageOperationException;
import com.part3_team4.deokhoogam.global.storage.FileUploader;
import jakarta.persistence.EntityManagerFactory;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DeleteOrphanThumbnailJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final OrphanThumbnailRepository orphanThumbnailRepository;
  private final EntityManagerFactory entityManagerFactory;
  private final FileUploader fileUploader;
  private final Clock clock;

  private final JobLoggingListener jobLoggingListener;
  private final SkipLoggingListener skipLoggingListener;
  private final BatchStepMetricListener batchStepMetricListener;
  private final BatchJobMetricListener batchJobMetricListener;

  private static final int CHUNK_SIZE = 100;

  @Bean
  public Job deleteOrphanThumbnailJob() {
    return new JobBuilder("deleteOrphanThumbnailJob", jobRepository)
        .listener(jobLoggingListener)
        .listener(batchJobMetricListener)
        .start(deleteOrphanThumbnailStep())
        .build();
  }

  @Bean
  public Step deleteOrphanThumbnailStep() {
    return new StepBuilder("deleteOrphanThumbnailStep", jobRepository)
        .<OrphanThumbnail, OrphanThumbnail>chunk(CHUNK_SIZE, transactionManager)
        .reader(orphanThumbnailReader())
        .processor(orphanThumbnailProcessor())
        .writer(orphanThumbnailWriter())
        .faultTolerant()
        .skip(StorageOperationException.class)
        .skipLimit(100)
        .listener(skipLoggingListener)
        .listener(batchStepMetricListener)
        .build();
  }

  @Bean
  @StepScope
  public JpaCursorItemReader<OrphanThumbnail> orphanThumbnailReader() {
    Instant targetTime = Instant.now(clock).minus(1, ChronoUnit.DAYS);
    return new JpaCursorItemReaderBuilder<OrphanThumbnail>()
        .name("orphanThumbnailReader")
        .entityManagerFactory(entityManagerFactory)
        .queryString(
            """
                    SELECT o
                    FROM OrphanThumbnail o
                    WHERE o.createdAt < :targetTime
                """
        )
        .parameterValues(Map.of("targetTime", targetTime))
        .saveState(false)
        .build();
  }

  @Bean
  public ItemWriter<OrphanThumbnail> orphanThumbnailWriter() {
    return chunk -> {
      List<OrphanThumbnail> items = new ArrayList<>(chunk.getItems());
      orphanThumbnailRepository.deleteAllInBatch(items);
    };
  }

  @Bean
  public ItemProcessor<OrphanThumbnail, OrphanThumbnail> orphanThumbnailProcessor() {
    return orphanThumbnail -> {
      log.debug("외부 자원 삭제 시도 - URL: {}", orphanThumbnail.getFileUrl());
      fileUploader.delete(orphanThumbnail.getFileUrl());
      return orphanThumbnail;
    };
  }
}