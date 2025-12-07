package io.github.jihyundev.spring_batch_etl.batch.scheduler;

import io.github.jihyundev.spring_batch_etl.domain.mall.MallConfig;
import io.github.jihyundev.spring_batch_etl.mapper.mall.MallConfigMapper;
import io.github.jihyundev.spring_batch_etl.service.MallConfigService;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberJobScheduler {
    private final JobLauncher jobLauncher;
    private final Job memberSyncJob;
    private final MallConfigMapper mallConfigMapper;

    private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * 전날 가입한 회원 정보 동기화
     */
    @Scheduled(cron = "0 47 20 * * *", zone = "Asia/Seoul")
    public void runDailyMemberSyncJob() {
        LocalDate today = LocalDate.now(ZONE_SEOUL);
        LocalDateTime startDate = today.minusDays(1).atStartOfDay();
        LocalDateTime endDate = today.atStartOfDay();

        mallConfigMapper.findAllMallIds().forEach(mallId -> {
            runMemberSyncJob(mallId, startDate, endDate, 1000);
        });
    }

    public void runMemberSyncJob(Long mallId, LocalDateTime startDate, LocalDateTime endDate, Integer pageSize) {
        String startDateStr = startDate.format(FORMATTER);
        String endDateStr = endDate.format(FORMATTER);

        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("mallId", mallId)
                .addString("startDate", startDateStr)
                .addString("endDate", endDateStr)
                .addLong("pageSize", pageSize != null ? pageSize.longValue() : 1000L)
                .addLong("timestamp", System.currentTimeMillis()) //RunIdIncrementer와 별개로 항상 새로운 JobInstance 생성, .incrementer(new RunIdIncrementer())와 중복됨 - 주석처리
                .toJobParameters();

        try {
            log.info("[MemberJobScheduler] run memberSyncJob mallId={}, start={}, end={}, pageSize={}", mallId, startDate, endDate, pageSize);

            JobExecution execution = jobLauncher.run(memberSyncJob, jobParameters);

            log.info("[MemberJobScheduler] Job finished. id={}, status={}", execution.getId(), execution.getStatus());
            //log.info("[MemberJobScheduler] Job started asynchronously. id={}, status={}", execution.getId(), execution.getStatus());
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException e) {
            log.warn("[MemberJobScheduler] Job not started. reason={}", e.getClass().getSimpleName(), e);
        } catch (Exception e) {
            log.error("[MemberJobScheduler] Unexpected error while running memberSyncJob", e);
        }

    }

}
