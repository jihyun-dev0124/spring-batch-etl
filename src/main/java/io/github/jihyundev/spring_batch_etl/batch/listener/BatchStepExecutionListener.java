package io.github.jihyundev.spring_batch_etl.batch.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.stereotype.Component;


//Step 단위 로깅/모니터링
@Slf4j
@Component
public class BatchStepExecutionListener implements StepExecutionListener {

    private static final int FAIL_THRESHOLD = 10; //실패 허용 건수, 품질 기준

    @Override
    public void beforeStep(StepExecution stepExecution) {
        log.info("---- Step [{}] START ----", stepExecution.getStepName());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        int writeSkip = (int) stepExecution.getWriteSkipCount();
        int processSkip = (int) stepExecution.getProcessSkipCount();
        int readSkip = (int) stepExecution.getReadSkipCount();
        int totalSkip = writeSkip + processSkip + readSkip;

        log.info("---- Step [{}] END ----", stepExecution.getStepName());
        log.info("READ={}, WRITE={}, SKIP(total)={}", stepExecution.getReadCount(), stepExecution.getWriteCount(), totalSkip);

        if (totalSkip >= FAIL_THRESHOLD) {
            log.error("[Step FAILED] skipCount={} >= threshold={}", totalSkip, FAIL_THRESHOLD);
            return ExitStatus.FAILED; //step 끝난 후 Step 상태 -> 실패처리
        }

        return stepExecution.getExitStatus();
    }
}
