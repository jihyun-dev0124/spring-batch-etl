package io.github.jihyundev.spring_batch_etl.batch.job;

import io.github.jihyundev.spring_batch_etl.api.MallApiClient;
import io.github.jihyundev.spring_batch_etl.api.dto.member.ApiMallMemberDto;
import io.github.jihyundev.spring_batch_etl.batch.exception.InvalidMemberDataException;
import io.github.jihyundev.spring_batch_etl.batch.exception.TransientApiException;
import io.github.jihyundev.spring_batch_etl.batch.etl.processor.MemberItemProcessor;
import io.github.jihyundev.spring_batch_etl.batch.etl.reader.MemberApiItemReader;
import io.github.jihyundev.spring_batch_etl.batch.etl.writer.MemberItemWriter;
import io.github.jihyundev.spring_batch_etl.batch.listener.MemberJobExecutionListener;
import io.github.jihyundev.spring_batch_etl.batch.listener.MemberSkipListener;
import io.github.jihyundev.spring_batch_etl.batch.listener.MemberStepExecutionListener;
import io.github.jihyundev.spring_batch_etl.domain.mall.MallConfig;
import io.github.jihyundev.spring_batch_etl.domain.member.MallMember;
import io.github.jihyundev.spring_batch_etl.mapper.mall.MallMemberMapper;
import io.github.jihyundev.spring_batch_etl.service.MallConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MemberSyncJobConfig {

    private final MallApiClient mallApiClient;
    private final RetryTemplate apiRetryTemplate;
    private final MallConfigService mallConfigService;
    private final MallMemberMapper mallMemberMapper;

    //Reader Bean
    @Bean
    @StepScope
    public MemberApiItemReader memberApiItemReader(
            @Value("#{jobParameters['mallId']}") Long mallId,
            @Value("#{jobParameters['pageSize']}") Integer pageSize,
            @Value("#{jobParameters['startDate']}") String startDateStr,
            @Value("#{jobParameters['endDate']}") String endDateStr
    ) {
        MallConfig mallConfig = mallConfigService.getMallConfig(mallId);
        return new MemberApiItemReader(
                mallApiClient,
                mallConfig,
                apiRetryTemplate,
                pageSize,
                startDateStr,
                endDateStr
        );
    }

    //Processor Bean
    @Bean
    @StepScope
    public MemberItemProcessor memberItemProcessor(@Value("#{jobParameters['mallId']}") Long mallId) {
        MallConfig mallConfig = mallConfigService.getMallConfig(mallId);
        return new MemberItemProcessor(mallConfig);
    }

    //Writer Bean
    @Bean
    public MemberItemWriter memberItemWriter() {
        return new MemberItemWriter(mallMemberMapper);
    }

    //Step Bean
    @Bean
    public Step memberSyncStep(JobRepository jobRepository,
                               PlatformTransactionManager transactionManager,
                               MemberApiItemReader memberApiItemReader,
                               MemberItemProcessor memberItemProcessor,
                               MemberItemWriter memberItemWriter,
                               MemberStepExecutionListener memberStepExecutionListener,
                               MemberSkipListener memberSkipListener) {

        return new StepBuilder("memberSyncStep", jobRepository)
                .<ApiMallMemberDto, MallMember>chunk(500, transactionManager)
                .reader(memberApiItemReader)
                .processor(memberItemProcessor)
                .writer(memberItemWriter)

                .faultTolerant()

                //1. 재시도 정책 - processor, writer에서 오류 발생시 같은 chunk 재시도, 3번 넘게 실패하면 skip 또는 Step 실패
                .retry(TransientApiException.class) //외부 API 일시적 오류
                .retry(TransientDataAccessException.class) // DB deadlock 등,,
                .retryLimit(3) //최대 3번까지 재시도

                //2. 스킵 정책 (재시도해도 안고쳐지는 에러들)
                .skip(InvalidMemberDataException.class) //데이터 자체가 잘못된 경우
                .skip(IllegalArgumentException.class) //잘못된 매핑 등
                .skipLimit(1000)       //최대 skip 허용 건수, 넘으면 즉시 배치 중단

                .listener(memberStepExecutionListener) //Step 단위 요약 + 실패 기준 ExitStatus 처리
                .listener(memberSkipListener) //개별 실패 row DB 백업
                .build();

    }

    //Job Bean
    @Bean
    public Job memberSyncJob(JobRepository jobRepository, Step memberSyncStep, MemberJobExecutionListener memberJobExecutionListener) {
        return new JobBuilder("memberSyncJob", jobRepository)
                .incrementer(new RunIdIncrementer()) //매 실행마다 run.id 증가
                .listener(memberJobExecutionListener)
                .start(memberSyncStep)
                .build();
    }
}
