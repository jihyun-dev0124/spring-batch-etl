package io.github.jihyundev.spring_batch_etl.batch.listener;

import io.github.jihyundev.spring_batch_etl.api.dto.member.ApiMallMemberDto;
import io.github.jihyundev.spring_batch_etl.batch.etl.writer.ErrorRowWriter;
import io.github.jihyundev.spring_batch_etl.domain.batch.BatchDomainType;
import io.github.jihyundev.spring_batch_etl.domain.member.MallMember;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberSkipListener implements SkipListener<ApiMallMemberDto, MallMember> , StepExecutionListener {
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final ErrorRowWriter errorRowWriter;

    //Step/Job/Parameter 정보 저장용
    private String jobName;
    private String stepName;
    private Long mallId;
    private LocalDate bizDate;

    // Processor에서 예외 발생하여 skip 된 경우
    @Override
    public void onSkipInProcess(ApiMallMemberDto item, Throwable t) {
        log.warn("[SKIP IN PROCESS] memberId={} reason={}", item.getMemberId(), t.getMessage());
        errorRowWriter.writerError(item, "PROCESS_ERROR", t, jobName, stepName, BatchDomainType.MEMBER, mallId, bizDate);
    }

    // Writer에서 예외 발생하여 skip 된 경우
    @Override
    public void onSkipInWrite(MallMember item, Throwable t) {
        log.warn("[SKIP IN WRITE] memberId={} reason={}", item.getMemberId(), t.getMessage());
        errorRowWriter.writerError(item, "WRITE_ERROR", t, jobName, stepName, BatchDomainType.MEMBER, mallId, bizDate);
    }

    // Reader에서 예외 방생하여 skip 된 경우
    @Override
    public void onSkipInRead(Throwable t) {
        log.warn("[SKIP IN READ] reason={}", t.getMessage());
        errorRowWriter.writerError(null, "READ_ERROR", t, jobName, stepName, BatchDomainType.MEMBER, mallId, bizDate);
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        this.stepName = stepExecution.getStepName();
        this.jobName = stepExecution.getJobExecution().getJobInstance().getJobName();

        JobParameters jobParameters = stepExecution.getJobParameters();
        this.mallId = jobParameters.getLong("mallId");

        String startDateStr = jobParameters.getString("startDate");
        if(startDateStr != null) {
            this.bizDate = LocalDate.parse(startDateStr, DATE_TIME_FMT);
        }else {
            this.bizDate = null;
        }

        log.info("[MemberSkipListener] initialized. jobName={}, stepName={}, mallId={}, bizDate={}",
                jobName, stepName, mallId, bizDate);
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        return stepExecution.getExitStatus();
    }


}
