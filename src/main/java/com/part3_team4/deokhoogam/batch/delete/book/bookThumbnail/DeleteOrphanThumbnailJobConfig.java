package com.part3_team4.deokhoogam.batch.delete.book.bookThumbnail;

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

@Configuration
@RequiredArgsConstructor
public class DeleteOrphanThumbnailJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final OrphanThumbnailRepository orphanThumbnailRepository;
  private final EntityManagerFactory entityManagerFactory;
  private final FileUploader fileUploader;
  private final Clock clock;

  private static final int CHUNK_SIZE = 100;

  @Bean
  public Job deleteOrphanThumbnailJob() {
    return new JobBuilder("deleteOrphanThumbnailJob", jobRepository)
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
      fileUploader.delete(
          orphanThumbnail.getFileUrl()
      );
      return orphanThumbnail;
    };
  }
}