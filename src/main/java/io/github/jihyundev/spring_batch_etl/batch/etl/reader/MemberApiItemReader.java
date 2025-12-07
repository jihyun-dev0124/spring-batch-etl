package io.github.jihyundev.spring_batch_etl.batch.etl.reader;

import io.github.jihyundev.spring_batch_etl.api.MallApiClient;
import io.github.jihyundev.spring_batch_etl.api.dto.member.ApiMallMemberDto;
import io.github.jihyundev.spring_batch_etl.batch.exception.TransientApiException;
import io.github.jihyundev.spring_batch_etl.domain.mall.MallConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;

@Slf4j
public class MemberApiItemReader implements ItemStreamReader<ApiMallMemberDto> {

    private static final String KEY_OFFSET = "mallMember.currentOffset";
    private static final String KEY_TOTAL_COUNT = "mallMember.totalCount";

    private final MallApiClient mallApiClient;
    private final MallConfig mallConfig;
    private final RetryTemplate apiRetryTemplate;
    private final LocalDateTime startDate;
    private final LocalDateTime endDate;
    private final int pageSize; //limit, 최대 1000

    //내부 상태
    private long totalCount = 0L; //전체 건수
    private int currentOffset = 0; //지금까지 반환한 개수
    private List<ApiMallMemberDto> buffer = Collections.emptyList();
    private int indexInBuffer = 0; // buffer 내 인덱스


    public MemberApiItemReader(MallApiClient mallApiClient,
                               MallConfig mallConfig,
                               RetryTemplate apiRetryTemplate,
                               @Value("#{jobParameters['pageSize'] ?: 1000}") Integer pageSize,
                               @Value("#{jobParameters['startDate']}") String startDateStr,
                               @Value("#{jobParameters['endDate']}") String endDateStr){
        this.mallApiClient = mallApiClient;
        this.mallConfig = mallConfig;
        this.apiRetryTemplate = apiRetryTemplate;
        this.pageSize = (pageSize != null && pageSize > 0) ? Math.min(pageSize, 1000) : 1000;
        this.startDate = parseDateTime(startDateStr);
        this.endDate = parseDateTime(endDateStr);

        log.info("[MemberApiItemReader] init mallId={}, pageSize={}, startDate={}, endDate={}", mallConfig.getMallId(), this.pageSize, this.startDate, this.endDate);
    }

    @Override
    public ApiMallMemberDto read() {
        //전체를 다 읽었으면 종료
        if(totalCount > 0 && currentOffset >= totalCount) {
            return null;
        }

        //아직 totalCount를 모르는 상태(open 안탔다거나)라면 방어적으로 한 번 더 체크
        if (totalCount == 0) {
            log.debug("[MemberApiItemReader] totalCount is 0, no data to read.");
            return null;
        }

        //현재 버퍼 다 썼으면 -> 다음 offset으로 새로 가져오기
        if(buffer == null || indexInBuffer >= buffer.size()) {
            fillBuffer();
        }

        if (buffer == null || buffer.isEmpty()) {
            //count 기준으로 남은 데이터가 있어야 하는데 API가 빈 리스트 주는 경우
            // API쪽 데이터 변경 등 예외 상황 -> 로그 남기고 종료 처리
            if (currentOffset < totalCount) {
                log.warn("[MemberApiItemReader] empty buffer while offset < totalCount. offset={}, totalCount={}", currentOffset, totalCount);
            }
            return null;
        }

        ApiMallMemberDto item = buffer.get(indexInBuffer++);
        currentOffset++;
        return item;
    }

    /**
     * 버퍼 채우기
     */
    private void fillBuffer() {
        if (currentOffset >= totalCount) {
            buffer = Collections.emptyList();
            indexInBuffer = 0;
            return;
        }

        int remaining = (int) Math.min(pageSize, totalCount - currentOffset);
        log.debug("[MemberApiItemReader] fetch members offset={}, limit={}, totalCount={}", currentOffset, remaining, totalCount);

        try {

            List<ApiMallMemberDto> result = apiRetryTemplate.execute(context -> {
                int attempt = context.getRetryCount() + 1;
                log.debug("[MemberApiItemReader] call external getMembers. attempt={}, offset={}, limit={}", attempt, currentOffset, remaining);
                return mallApiClient.getMembers(mallConfig, currentOffset, remaining, startDate, endDate);
            });

            if(result == null || result.isEmpty()) {
                buffer = Collections.emptyList();
                indexInBuffer = 0;
            }else{
                buffer = result;
                indexInBuffer = 0;
                log.info("[MemberApiItemReader] fetched {} members (offset={}, limit={})", buffer.size(), currentOffset, remaining);
            }
        } catch (WebClientResponseException | WebClientRequestException e) {
            //일시적인 네트워크/서버 문제라고 판단되면 TransientApiException으로 래핑
            throw new TransientApiException("External member API call failed. offset=" + currentOffset, e);
        } catch (Exception e) {
            //그 외 예외는 그대로 던져서 Step 실패로 처리 (정책에 따라 조정 가능)
            throw e;
        }

    }

    // ExecutionContext (재시작 지원)
    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        if (executionContext.containsKey(KEY_TOTAL_COUNT)) {
            this.totalCount = executionContext.getLong(KEY_TOTAL_COUNT);
            this.currentOffset = executionContext.getInt(KEY_OFFSET);
            log.info("[MemberApiItemReader] open with existing context: totalCount={}, currentOffset={}",
                    totalCount, currentOffset);
        } else {
            //첫 실행 : count API 호출, 실패 시 3번 재시도
            this.totalCount = apiRetryTemplate.execute(context -> {
                log.debug("[MemberApiItemReader] getMemberCount attempt={}", context.getRetryCount() + 1);
                return (long) mallApiClient.getMemberCount(mallConfig, startDate, endDate);
            });

            this.currentOffset = 0;
            log.info("[MemberApiItemReader] open fresh: totalCount={}, currentOffset={}", totalCount, currentOffset);
        }
        this.buffer = Collections.emptyList();
        this.indexInBuffer = 0;
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        executionContext.putLong(KEY_TOTAL_COUNT, this.totalCount);
        executionContext.putInt(KEY_OFFSET, this.currentOffset);
        // buffer 내용까지 저장하지는 않으면 → 재시작 시 해당 offset부터 다시 API 호출
        log.debug("[MemberApiItemReader] update context: totalCount={}, currentOffset={}", totalCount, currentOffset);
    }

    @Override
    public void close() throws ItemStreamException {
        log.info("[MemberApiItemReader] close called. totalCount={}, currentOffset={}", totalCount, currentOffset);
        this.buffer = Collections.emptyList();
    }

    private LocalDateTime parseDateTime(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        try {
            return LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e) {
            LocalDate date = LocalDate.parse(text, DateTimeFormatter.ISO_LOCAL_DATE);
            return date.atStartOfDay(); //00:00:00 적용
        }
    }

}
