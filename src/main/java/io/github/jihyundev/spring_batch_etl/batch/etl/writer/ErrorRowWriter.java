package io.github.jihyundev.spring_batch_etl.batch.etl.writer;

import io.github.jihyundev.spring_batch_etl.api.dto.member.ApiMallMemberDto;
import io.github.jihyundev.spring_batch_etl.api.dto.sales.ApiSalesDto;
import io.github.jihyundev.spring_batch_etl.common.JsonConverter;
import io.github.jihyundev.spring_batch_etl.domain.batch.BatchDomainType;
import io.github.jihyundev.spring_batch_etl.domain.batch.BatchErrorLog;
import io.github.jihyundev.spring_batch_etl.domain.member.MallMember;
import io.github.jihyundev.spring_batch_etl.mapper.batch.BatchErrorLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class ErrorRowWriter {
    private final BatchErrorLogMapper batchErrorLogMapper;
    private final JsonConverter jsonConverter;

    public void writerError(Object item, String errorType, Throwable t, String jobName, String stepName, BatchDomainType domainType, Long mallId, LocalDate bizDate) {
        String rawPayload = jsonConverter.convertItemToJson(item);
        BatchErrorLog log = BatchErrorLog
                .builder()
                .jobName(jobName)
                .stepName(stepName)
                .domainType(domainType)
                .mallId(mallId)
                .entityKey(extractEntityKey(item))
                .bizDate(bizDate)
                .errorType(errorType)
                .errorMessage(limitMessage(t.getMessage(), 2000))
                .rawPayload(rawPayload)
                .retryFlag(0)
                .retryCount(0)
                .build();

        batchErrorLogMapper.insertErrorLog(log);
    }

    private String limitMessage(String message, Integer limit) {
        if(message == null) return null;
        return message.length() > limit ? message.substring(0, limit) : message;
    }

    private String extractEntityKey(Object item) {
        if(item == null) return null;
        if(item instanceof ApiMallMemberDto dto) {
            return dto.getMemberId();
        }
        if (item instanceof MallMember entity) {
            return entity.getMemberId();
        }
        if(item instanceof ApiSalesDto sales) {
            return sales.getCollectionDate() + " " + sales.getCollectionHour();
        }
        return null;
    }
}
