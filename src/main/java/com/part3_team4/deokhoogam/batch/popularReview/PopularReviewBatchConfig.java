package com.part3_team4.deokhoogam.batch.popularReview;

import com.part3_team4.deokhoogam.domain.book.entity.PeriodType;

import lombok.RequiredArgsConstructor;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;


@Configuration
@RequiredArgsConstructor
public class PopularReviewBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final PopularReviewCalculator popularReviewCalculator;

    @Bean
    public Job popularReviewJob() {
        return new JobBuilder("popularReviewJob", jobRepository)
                .start(popularReviewStep())
                .build();
    }

    @Bean
    public Step popularReviewStep() {
        return new StepBuilder("popularReviewStep", jobRepository)
                .tasklet(popularReviewTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet popularReviewTasklet() {
        return (contribution, chunkContext) -> {
            for (PeriodType period : PeriodType.values()) {
                popularReviewCalculator.calculateAndSave(period);
            }
            return RepeatStatus.FINISHED;
        };
    }

}
