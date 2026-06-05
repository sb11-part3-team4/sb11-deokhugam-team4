package com.part3_team4.deokhoogam.batch.delete.user;

import com.part3_team4.deokhoogam.domain.user.entity.DeletedUser;
import com.part3_team4.deokhoogam.domain.user.repository.DeletedUserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class DeleteExpiredUserJobConfig {

  private final DeletedUserRepository deletedUserRepository;
  private final Clock clock;

  public DeleteExpiredUserJobConfig(DeletedUserRepository deletedUserRepository, Clock clock) {
    this.deletedUserRepository = deletedUserRepository;
    this.clock = clock;
  }

  @Bean
  public Job deleteExpiredUserJob(JobRepository jobRepository, Step deleteExpiredUserStep) {
    return new JobBuilder("deleteExpiredUserJob", jobRepository)
        .start(deleteExpiredUserStep)
        .build();
  }

  @Bean
  public Step deleteExpiredUserStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
    return new StepBuilder("deleteExpiredUserStep", jobRepository)
        .<DeletedUser, DeletedUser>chunk(100, transactionManager)
        .reader(expiredUserReader())
        .writer(expiredUserWriter())
        .build();
  }

  @Bean
  public RepositoryItemReader<DeletedUser> expiredUserReader() {
    Instant oneDayAgo = Instant.now(clock).minus(1, ChronoUnit.DAYS);

    return new RepositoryItemReaderBuilder<DeletedUser>()
        .name("expiredUserReader")
        .repository(deletedUserRepository)
        .methodName("findByDeletedAtBefore")
        .arguments(Collections.singletonList(oneDayAgo))
        .pageSize(100)
        .sorts(Collections.singletonMap("deletedAt", Sort.Direction.ASC))
        .build();
  }

  @Bean
  public ItemWriter<DeletedUser> expiredUserWriter() {
    return items -> deletedUserRepository.deleteAll(items);
  }
}