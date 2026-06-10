package com.part3_team4.deokhoogam.batch.delete.notification;

import com.part3_team4.deokhoogam.domain.notification.entity.Notification;
import com.part3_team4.deokhoogam.domain.notification.repository.NotificationRepository;
import jakarta.persistence.EntityManagerFactory;
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

import java.util.ArrayList;

@Configuration
@RequiredArgsConstructor
public class DeleteOrphanNotificationJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final NotificationRepository notificationRepository;

    private static final int CHUNK_SIZE = 100;

    private static final String ORPHAN_QUERY =
        "SELECT n FROM Notification n " +
        "WHERE NOT EXISTS (SELECT 1 FROM Review r WHERE r.id = n.reviewId) " +
        "   OR NOT EXISTS (SELECT 1 FROM User u WHERE u.id = n.userId)";

    @Bean
    public Job deleteOrphanNotificationJob() {
        return new JobBuilder("deleteOrphanNotificationJob", jobRepository)
            .start(deleteOrphanNotificationStep())
            .build();
    }

    @Bean
    public Step deleteOrphanNotificationStep() {
        return new StepBuilder("deleteOrphanNotificationStep", jobRepository)
            .<Notification, Notification>chunk(CHUNK_SIZE, transactionManager)
            .reader(orphanNotificationReader())
            .writer(orphanNotificationWriter())
            .build();
    }

    @Bean
    public JpaCursorItemReader<Notification> orphanNotificationReader() {
        return new JpaCursorItemReaderBuilder<Notification>()
            .name("orphanNotificationReader")
            .entityManagerFactory(entityManagerFactory)
            .queryString(ORPHAN_QUERY)
            .build();
    }

    @Bean
    public ItemWriter<Notification> orphanNotificationWriter() {
        return chunk -> notificationRepository.deleteAllInBatch(new ArrayList<>(chunk.getItems()));
    }
}