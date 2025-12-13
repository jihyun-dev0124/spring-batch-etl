package io.github.jihyundev.spring_batch_etl.batch.listener;

import io.github.jihyundev.spring_batch_etl.common.JsonConverter;
import io.github.jihyundev.spring_batch_etl.domain.batch.BatchDomainType;
import io.github.jihyundev.spring_batch_etl.domain.batch.BatchExecutionSummary;
import io.github.jihyundev.spring_batch_etl.mapper.batch.BatchErrorLogMapper;
import io.github.jihyundev.spring_batch_etl.mapper.batch.BatchExecutionSummaryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchExecutionSummaryListener implements JobExecutionListener {
    private final BatchExecutionSummaryMapper batchExecutionSummaryMapper;
    private final BatchErrorLogMapper batchErrorLogMapper;
    private final JsonConverter jsonConverter;

    @Override
    public void afterJob(JobExecution jobExecution) {
        try {
            BatchExecutionSummary summary = buildSummary(jobExecution);
            batchExecutionSummaryMapper.insertBatchExecutionSummary(summary);
        } catch (Exception e) {
            log.error("Failed to save batch execution summary", e);
        }
    }

    private BatchExecutionSummary buildSummary(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        Long jobInstanceId = jobExecution.getJobInstance().getInstanceId();
        Long jobExecutionId = jobExecution.getId();
        
        //job parameter값 파싱
        JobParameters params = jobExecution.getJobParameters();
        Long mallId = params.getLong("mallId");
        BatchDomainType domainType = BatchDomainType.valueOf(params.getString("domainType"));
        LocalDateTime fromDate = Optional.ofNullable(params.getString("startDate")).map(LocalDateTime::parse).orElse(null);
        LocalDateTime toDate = Optional.ofNullable(params.getString("endDate")).map(LocalDateTime::parse).orElse(null);
        
        //StepExecution 합산
        int readCount = 0;
        int writeCount = 0;
        int filterCount = 0;
        int readSkip = 0;
        int processSkip = 0;
        int writeSkip = 0;
        String mainStepName = null;

        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
            if (mainStepName == null) {
                mainStepName = stepExecution.getStepName();
            }
            readCount += stepExecution.getReadCount();
            writeCount += stepExecution.getWriteCount();
            filterCount += stepExecution.getFilterCount();
            readSkip += stepExecution.getReadSkipCount();
            processSkip += stepExecution.getProcessSkipCount();
            writeSkip += stepExecution.getWriteSkipCount();
        }

        //에러 로그 카운트
        int errorLogCount = batchErrorLogMapper.countByExecutionContext(jobName, domainType.toString(), mallId, fromDate, toDate);

        LocalDateTime startTime = jobExecution.getStartTime();
        LocalDateTime endTime = jobExecution.getEndTime();
        long durationMs = (startTime != null && endTime != null) ? Duration.between(startTime, endTime).toMillis() : 0L;

        String status = jobExecution.getStatus().toString();
        String exitCode = jobExecution.getExitStatus().getExitCode();
        String exitMsg = jobExecution.getExitStatus().getExitDescription();

        boolean successFlag = computeSuccessFlag(status, readSkip, processSkip, writeSkip);
        String parameterJson = jsonConverter.convertItemToJson(params);

        return BatchExecutionSummary.builder()
                .jobName(jobName)
                .stepName(mainStepName)
                .domainType(domainType)
                .mallId(mallId)
                .bizDateFrom(fromDate)
                .bizDateTo(toDate)
                .jobInstanceId(jobInstanceId)
                .jobExecutionId(jobExecutionId)
                .parameterJson(parameterJson)
                .status(status)
                .exitCode(exitCode)
                .exitMessage(exitMsg)
                .readCount(readCount)
                .writeCount(writeCount)
                .filterCount(filterCount)
                .readSkipCount(readSkip)
                .processSkipCount(processSkip)
                .writeSkipCount(writeSkip)
                .errorLogCount(errorLogCount)
                .startTime(startTime)
                .endTime(endTime)
                .durationMs(durationMs)
                .successFlag(successFlag ? 1 : 0)
                .build();
    }

    private boolean computeSuccessFlag(String status, int readSkip, int processSkip, int writeSkip) {
        if(!"COMPLETED".equals(status)) return false;
        int totalSkip = readSkip + processSkip + writeSkip;
        return totalSkip < 10; //skip 10건 미만이면 성공 처리
    }
}
