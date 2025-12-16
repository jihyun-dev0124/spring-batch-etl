package io.github.jihyundev.spring_batch_etl.service;

import io.github.jihyundev.spring_batch_etl.domain.batch.BatchDomainType;
import io.github.jihyundev.spring_batch_etl.domain.batch.BatchRetryRequest;
import io.github.jihyundev.spring_batch_etl.mapper.batch.BatchRetryRequestMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchRetryService {
    private final JobLauncher jobLauncher;
    private final Job memberSyncJob;
    private final BatchRetryRequestMapper batchRetryRequestMapper;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public boolean retrySyncJob(Long jobExecutionId) {
        BatchRetryRequest batchRetryRequest = batchRetryRequestMapper.findByJobExecutionId(jobExecutionId);
        if (batchRetryRequest == null) {
            return false;
        }

        Long mallId = batchRetryRequest.getMallId();
        BatchDomainType domainType = batchRetryRequest.getDomainType();
        Long attemptCount = batchRetryRequest.getAttemptCount();
        LocalDateTime startDate = batchRetryRequest.getBizDateFrom();
        LocalDateTime endDate = batchRetryRequest.getBizDateTo();

        String startDateStr = startDate.format(FORMATTER);
        String endDateStr = endDate.format(FORMATTER);

        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("mallId", mallId)
                .addString("startDate", startDateStr)
                .addString("endDate", endDateStr)
                .addString("domainType", domainType.toString())
                .addLong("pageSize", 1000L)
                .addLong("attemptCount", attemptCount + 1) //job 실행 횟수
                .toJobParameters();

        try {
            if(domainType.equals(BatchDomainType.MEMBER)){
                JobExecution execution = jobLauncher.run(memberSyncJob, jobParameters);
            }
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException e) {
            log.warn("[retryJob] Job not started. reason={}", e.getClass().getSimpleName(), e);
            return false;
        } catch (Exception e) {
            log.error("[retryJob] Unexpected error while running memberSyncJob", e);
            return false;
        }

        return true;
    }
}
