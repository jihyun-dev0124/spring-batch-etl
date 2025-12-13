package io.github.jihyundev.spring_batch_etl.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BatchInfraConfig extends DefaultBatchConfiguration {
    private final DataSource dataSource;

    /**
     * Batch에서 사용할 트랜잭션 매니저
     * @return
     */
    @Bean
    public PlatformTransactionManager transactionManager() {
        return new JdbcTransactionManager(dataSource);
    }

    /**
     * 배치용 Thread Pool
     * - Step 병렬 처리, partition, 멀티 스텝 실행 등에 재사용 가능
     * @return
     */
    @Bean
    public TaskExecutor batchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("batch-task-");
        executor.initialize();
        return executor;
    }

    @Bean
    public JobLauncher jobLauncher(JobRepository jobRepository, TaskExecutor batchTaskExecutor) throws Exception {
        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.setTaskExecutor(batchTaskExecutor); //batchTaskExecutor - 비동기 설정, null - 동기 설정
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }

}
