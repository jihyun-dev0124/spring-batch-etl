package io.github.jihyundev.spring_batch_etl.domain.batch;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@ToString
public class BatchErrorLog {
    Long id;
    String jobName;
    String stepName;
    BatchDomainType domainType;
    Long mallId;
    String entityKey;
    LocalDate bizDate;
    String errorType;
    String errorMessage;
    String rawPayload;
    int retryFlag; //0:미재처리, 1:재처리 요청
    int retryCount; //재처리 count
    LocalDateTime createdAt;
}
