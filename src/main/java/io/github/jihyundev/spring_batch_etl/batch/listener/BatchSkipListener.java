package io.github.jihyundev.spring_batch_etl.batch.listener;

import io.github.jihyundev.spring_batch_etl.api.dto.member.ApiMallMemberDto;
import io.github.jihyundev.spring_batch_etl.batch.etl.writer.ErrorRowWriter;
import io.github.jihyundev.spring_batch_etl.domain.batch.BatchDomainType;
import io.github.jihyundev.spring_batch_etl.domain.member.MallMember;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchSkipListener implements SkipListener<ApiMallMemberDto, MallMember> , StepExecutionListener {
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final ErrorRowWriter errorRowWriter;

    // Processor에서 예외 발생하여 skip 된 경우
    @Override
    public void onSkipInProcess(ApiMallMemberDto item, Throwable t) {
        skipWriteError(item, "PROCESS_ERROR", t);
    }

    // Writer에서 예외 발생하여 skip 된 경우
    @Override
    public void onSkipInWrite(MallMember item, Throwable t) {
        skipWriteError(item, "WRITE_ERROR", t);
    }

    // Reader에서 예외 방생하여 skip 된 경우
    @Override
    public void onSkipInRead(Throwable t) {
        skipWriteError(null, "READ_ERROR", t);
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        JobParameters params = stepExecution.getJobParameters();
        stepExecution.getExecutionContext().putLong("mallId", params.getLong("mallId"));
        stepExecution.getExecutionContext().putString("domainType", params.getString("domainType"));
        stepExecution.getExecutionContext().putString("jobName", stepExecution.getJobExecution().getJobInstance().getJobName());
        stepExecution.getExecutionContext().putString("stepName", stepExecution.getStepName());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        return stepExecution.getExitStatus();
    }

    public void skipWriteError(Object item, String errorType, Throwable t){
        StepExecution stepExecution = StepSynchronizationManager.getContext().getStepExecution();
        JobParameters jobParameters = stepExecution.getJobParameters();
        ExecutionContext ec = stepExecution.getExecutionContext();

        Long mallId = ec.getLong("mallId");
        String jobName = ec.getString("jobName");
        String stepName = ec.getString("stepName");
        BatchDomainType domainType = jobParameters.getString("domainType") != null ? BatchDomainType.valueOf(jobParameters.getString("domainType")) : null;
        String startDateStr = jobParameters.getString("startDate");
        LocalDate bizDate = startDateStr != null ? LocalDate.parse(startDateStr, DATE_TIME_FMT) : null;

        log.warn("[SKIP {}] mallId={} reason={}", errorType, mallId, t.getMessage());
        errorRowWriter.writerError(item, errorType, t, jobName, stepName, domainType, mallId, bizDate);
    }

}
