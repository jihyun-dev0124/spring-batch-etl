package io.github.jihyundev.spring_batch_etl.batch.retry;

import io.github.jihyundev.spring_batch_etl.batch.exception.TransientApiException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ApiRetryConfig {

    private static final int RETRY_COUNT = 3;
    private static final long BACKOFF_DELAY = 1000L;

    @Bean
    public RetryTemplate apiRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        Map<Class<? extends Throwable>, Boolean> retryable = new HashMap<>();
        retryable.put(TransientApiException.class, true); //TransientApiException 일때만 재시도
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(RETRY_COUNT, retryable, true); //최대 재시도 횟수(3번)

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(BACKOFF_DELAY); //재시도 사이 1초 대기

        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        return retryTemplate;
    }
}
