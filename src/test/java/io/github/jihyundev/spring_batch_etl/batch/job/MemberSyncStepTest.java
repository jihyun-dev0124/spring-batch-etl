package io.github.jihyundev.spring_batch_etl.batch.job;

import io.github.jihyundev.spring_batch_etl.api.MallApiClient;
import io.github.jihyundev.spring_batch_etl.api.dto.member.ApiMallMemberDto;
import io.github.jihyundev.spring_batch_etl.batch.exception.TransientApiException;
import io.github.jihyundev.spring_batch_etl.domain.batch.BatchDomainType;
import io.github.jihyundev.spring_batch_etl.domain.batch.BatchErrorLog;
import io.github.jihyundev.spring_batch_etl.domain.mall.MallConfig;
import io.github.jihyundev.spring_batch_etl.domain.member.MallMember;
import io.github.jihyundev.spring_batch_etl.mapper.mall.BatchErrorLogMapper;
import io.github.jihyundev.spring_batch_etl.mapper.mall.MallConfigMapper;
import io.github.jihyundev.spring_batch_etl.mapper.mall.MallMemberMapper;
import io.github.jihyundev.spring_batch_etl.service.MallConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBatchTest
@SpringBootTest
public class MemberSyncStepTest {
    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private Job memberSyncJob;

    @Autowired
    private MallMemberMapper mallMemberMapper;

    @Autowired
    private MallConfigMapper mallConfigMapper;

    @Autowired
    private BatchErrorLogMapper batchErrorLogMapper;

    @MockitoBean
    private MallApiClient mallApiClient;    //Reader stub

    private static final Long TEST_MALL_ID = 1L;
    private static String startDateStr = "2025-12-06T00:00:00";
    private static String endDateStr = "2025-12-07T00:00:00";

    @BeforeEach
    void setUp() {
        jobLauncherTestUtils.setJob(memberSyncJob);

        MallConfig mallConfig = new MallConfig();
        mallConfig.setMallId(TEST_MALL_ID);
        mallConfig.setMallName("테스트몰");
        mallConfig.setIsUsable("T");
        mallConfig.setCreatedAt(LocalDateTime.now());
        mallConfigMapper.insert(mallConfig);
    }

    //동기 JobLauncher 실행
    @TestConfiguration
    static class SyncJobLauncherTestConfig {
        @Bean
        @Primary
        public JobLauncher syncJobLauncher(JobRepository jobRepository) throws Exception {
            TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
            jobLauncher.setJobRepository(jobRepository);
            jobLauncher.afterPropertiesSet();
            return jobLauncher;
        }
    }

    @Test
    void memberSyncStep_h2_데이터_들어가는지_확인() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("mallId", TEST_MALL_ID)
                .addString("startDate", startDateStr)
                .addString("endDate", endDateStr)
                .addLong("pageSize", 1000L)
                .addLong("run.id", System.currentTimeMillis()) // RunIdIncrementer 대체용
                .toJobParameters();

        //read
        when(mallApiClient.getMemberCount(any(), any(), any())).thenReturn(3);
        when(mallApiClient.getMembers(any(), anyInt(), anyInt(), any(), any()))
                .thenReturn(makeDtoList(3));

        JobExecution execution = jobLauncherTestUtils.launchStep("memberSyncStep", jobParameters);
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        List<MallMember> allMembers = mallMemberMapper.findAll();
        assertThat(allMembers.size()).isEqualTo(3);

        MallMember m1 = mallMemberMapper.findByMallIdAndMemberId(TEST_MALL_ID, "TEST-1");
        assertThat(m1).isNotNull();
        assertThat(m1.getName()).isEqualTo("테스터1");
        assertThat(m1.getEmail()).isEqualTo("t_email1@example.com");

        MallMember m2 = mallMemberMapper.findByMallIdAndMemberId(TEST_MALL_ID, "TEST-2");
        assertThat(m2).isNotNull();
        assertThat(m2.getName()).isEqualTo("테스터2");
        assertThat(m2.getEmail()).isEqualTo("t_email2@example.com");
    }

    @Test
    void memberSyncStep_EndToEnd_테스트() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("mallId", 1L)
                .addString("startDate", startDateStr)
                .addString("endDate", endDateStr)
                .addLong("pageSize", 1000L)
                .toJobParameters();

        //read
        when(mallApiClient.getMemberCount(any(), any(), any())).thenReturn(3);
        when(mallApiClient.getMembers(any(), anyInt(), anyInt(), any(), any()))
                .thenReturn(makeDtoList(3));

        JobExecution jobExecution = jobLauncherTestUtils.launchStep("memberSyncStep", jobParameters);

        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
    }

    @Test
    void memberSyncStep_read_retry_2번_실패_후_성공하면_정상적으로_COMPLETED() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("mallId", 1L)
                .addString("startDate", startDateStr)
                .addString("endDate", endDateStr)
                .addLong("pageSize", 1000L)
                .toJobParameters();

        when(mallApiClient.getMemberCount(any(), any(), any())).thenReturn(3);

        AtomicInteger callCount = new AtomicInteger(0);
        when(mallApiClient.getMembers(any(), anyInt(), anyInt(), any(), any()
        )).thenAnswer(invocation -> {
            int n = callCount.incrementAndGet();
            if (n < 3) {
                throw new TransientApiException("temporary api error, call="+n);
            }
            return makeDtoList(3);
        });

        JobExecution execution = jobLauncherTestUtils.launchStep("memberSyncStep", jobParameters);
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

    @Test
    void memberSyncStep_process_실패시_skip_error_내용_저장() throws Exception {
        //given
        List<ApiMallMemberDto> memberDtos = makeDtoList(2);
        memberDtos.add(ApiMallMemberDto
                .builder()
                .memberId("TEST-3")
                .name("실패회원")
                .phone(null)    //processor -> 실패
                .email(null)
                .joinPath("P")
                .createdDate(startDateStr)
                .build()
        );

        when(mallApiClient.getMemberCount(any(), any(), any())).thenReturn(3);
        when(mallApiClient.getMembers(any(), anyInt(), anyInt(), any(), any()))
                .thenReturn(memberDtos);

        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("mallId", 1L)
                .addString("startDate", startDateStr)
                .addString("endDate", endDateStr)
                .addLong("pageSize", 1000L)
                .toJobParameters();

        //when
        JobExecution execution = jobLauncherTestUtils.launchStep("memberSyncStep", jobParameters);

        //then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        StepExecution stepExecution = execution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getReadCount()).isEqualTo(3);
        assertThat(stepExecution.getSkipCount()).isEqualTo(1);
        assertThat(stepExecution.getWriteCount()).isEqualTo(2);

        List<BatchErrorLog> errorLogs = batchErrorLogMapper.findAll();
        assertThat(errorLogs.size()).isEqualTo(1);

        BatchErrorLog log = errorLogs.get(0);
        System.out.println("log.toString() = " + log.toString());
        assertThat(log.getErrorType()).isEqualTo("PROCESS_ERROR");
        assertThat(log.getDomainType()).isEqualTo(BatchDomainType.MEMBER);
        assertThat(log.getEntityKey()).isEqualTo("TEST-3");
    }

    private List<ApiMallMemberDto> makeDtoList(int size) {
        List<ApiMallMemberDto> list = new ArrayList<>();
        for (int i = 1; i <= size; i++) {
            list.add(ApiMallMemberDto.builder()
                    .memberId("TEST-"+i)
                    .name("테스터"+i)
                    .phone("010-1234-56"+String.format("%02d", i))
                    .email("t_email"+i+"@example.com")
                    .joinPath("P")
                    .createdDate("2025-12-02T11:19:27+09:00")
                    .build());
        }

        return list;
    }

}


