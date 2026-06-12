package com.part3_team4.deokhoogam.batch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class BatchSchedulerTest {

    @InjectMocks
    BatchScheduler batchScheduler;

    @Mock
    JobLauncher jobLauncher;

    @Mock
    Job deleteOrphanCommentJob;

    @Mock
    Job deleteOrphanReviewJob;

    @Mock
    Job deleteOrphanNotificationJob;

    @Mock
    Job popularReviewJob;

    @Test
    @DisplayName("runPopularReviewJob 호출 시 popularReviewJob이 실행된다")
    void runPopularReviewJob_executesJob() throws Exception {
        batchScheduler.runPopularReviewJob();

        then(jobLauncher).should().run(eq(popularReviewJob), any(JobParameters.class));
    }
}
