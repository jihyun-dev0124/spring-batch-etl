package io.github.jihyundev.spring_batch_etl.batch.etl.reader;

import io.github.jihyundev.spring_batch_etl.api.MallApiClient;
import io.github.jihyundev.spring_batch_etl.api.dto.member.ApiMallMemberDto;
import io.github.jihyundev.spring_batch_etl.batch.etl.processor.MemberItemProcessor;
import io.github.jihyundev.spring_batch_etl.batch.exception.TransientApiException;
import io.github.jihyundev.spring_batch_etl.domain.mall.MallConfig;
import io.github.jihyundev.spring_batch_etl.util.DateUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class MemberApiItemReaderTest {
    @MockitoBean
    MallApiClient mallApiClient;

    private MallConfig mallConfig;

    private RetryTemplate apiRetryTemplate;

    @BeforeEach
    void setUp() {
        mallConfig = new MallConfig();
        mallConfig.setMallId(1L);

        apiRetryTemplate = new RetryTemplate();
        Map<Class<? extends Throwable>, Boolean> retryable = new HashMap<>();
        retryable.put(TransientApiException.class, true); //TransientApiException 일때만 재시도
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(3, retryable, true); //최대 재시도 횟수(3번)

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(1000); //재시도 사이 1초 대기

        apiRetryTemplate.setRetryPolicy(retryPolicy);
        apiRetryTemplate.setBackOffPolicy(backOffPolicy);
    }

    @Test
    void reader가_count기반으로_모든_데이터를_읽는지() {
        when(mallApiClient.getMemberCount(any(), any(), any()))
                .thenReturn(2500);

        when(mallApiClient.getMembers(any(), eq(0), eq(1000), any(), any()))
                .thenReturn(makeDtoList(1000));
        when(mallApiClient.getMembers(any(), eq(1000), eq(1000), any(), any()))
                .thenReturn(makeDtoList(1000));
        when(mallApiClient.getMembers(any(), eq(2000), eq(500), any(), any()))
                .thenReturn(makeDtoList(500));

        MemberApiItemReader reader = new MemberApiItemReader(mallApiClient, mallConfig, apiRetryTemplate,1000, "2025-12-02", "2025-12-03");
        ExecutionContext etc = new ExecutionContext();
        reader.open(etc);

        int count = 0;
        while (true) {
            ApiMallMemberDto memberDto = reader.read();
            if (memberDto == null) break;
            count++;
        }

        assertEquals(2500, count);
    }

    @Test
    void retry_2번_실패_후_성공하면_정상적으로_read() throws Exception{
        // given
        when(mallApiClient.getMemberCount(any(), any(), any()))
                .thenReturn(2);

        AtomicInteger callCount = new AtomicInteger(0);
        when(mallApiClient.getMembers(
                any(MallConfig.class), anyInt(), anyInt(), any(), any()
        )).thenAnswer(invocation -> {
            int n = callCount.incrementAndGet();
            if (n < 3) {
                throw new TransientApiException("temporary api error, call=" + n);
            }
            //3번째 호출에서 정상 응답
            return makeDtoList(2);
        });

        MemberApiItemReader reader = new MemberApiItemReader(mallApiClient, mallConfig, apiRetryTemplate, 1000, "2025-12-02T00:00:00", "2025-12-03T00:00:00");
        ExecutionContext etc = new ExecutionContext();

        // when
        reader.open(etc);

        ApiMallMemberDto m1 = reader.read();
        ApiMallMemberDto m2 = reader.read();
        ApiMallMemberDto m3 = reader.read(); //null

        // then
        assertThat(m1).isNotNull();
        assertThat(m2).isNotNull();
        assertThat(m3).isNull();

        verify(mallApiClient, times(1)).getMemberCount(any(), any(), any());
        //members는 3번만에 성공 (2번 실패 + 1번 성공)
        verify(mallApiClient, times(3)).getMembers(any(), anyInt(), anyInt(), any(), any());

    }

    private List<ApiMallMemberDto> makeDtoList(int size) {
        List<ApiMallMemberDto> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
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