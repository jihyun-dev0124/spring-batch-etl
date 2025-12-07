package io.github.jihyundev.spring_batch_etl.batch.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

// Job 시작/종료 로깅
@Slf4j
@Component
public class MemberJobExecutionListener implements JobExecutionListener {
    private LocalDateTime startTime;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        this.startTime = LocalDateTime.now();
        log.info("==== [MemberSyncJob] START ====");
        log.info("job Name :{}", jobExecution.getJobInstance().getJobName());
        log.info("job Parameters :{}", jobExecution.getJobParameters());
        log.info("Start Time :{}", startTime);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        LocalDateTime endTime = LocalDateTime.now();
        Duration duration = Duration.between(startTime, endTime);

        //Step 중 하나라도 실패하면 Job 전체 FAILED 처리
        boolean stepFailed = jobExecution.getStepExecutions().stream()
                .anyMatch(step -> step.getExitStatus().equals(ExitStatus.FAILED));
        if (stepFailed) {
            jobExecution.setStatus(BatchStatus.FAILED);
            jobExecution.setExitStatus(ExitStatus.FAILED);
        }

        log.info("==== [MemberSyncJob] END ====");
        log.info("Exit Status :{}", jobExecution.getExitStatus());
        log.info("End Time :{}", endTime);
        log.info("Duration :{}", duration);

        // StepExecution 들의 요약 출력
        jobExecution.getStepExecutions().forEach(step ->{
            log.info("Step [{}] - READ: {}, WRITE: {}, SKIP: {}",
                    step.getStepName(),
                    step.getReadCount(),
                    step.getWriteCount(),
                    step.getSkipCount());
        });

        //실패한 경우 에러 메시지 출력
        if(!jobExecution.getAllFailureExceptions().isEmpty()) {
            log.error("Job Failed with Execptions:");
            jobExecution.getAllFailureExceptions().forEach(ex ->
                    log.error("Exception:", ex)
            );
        }

        // Slack, Email 등 알림
    }
}
