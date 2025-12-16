package io.github.jihyundev.spring_batch_etl.domain.batch;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@Builder
@ToString
public class BatchRetryRequest {
    private Long id;
    private String jobName;
    private Long jobInstanceId;
    private Long jobExecutionId;
    private Long mallId;
    private BatchDomainType domainType;
    private LocalDateTime bizDateFrom;
    private LocalDateTime bizDateTo;
    private String parameterJson;
    private String status; //REQUESTED, RUNNING, FAILED, DONE
    private Long attemptCount;
    private String requestedBy;
    private LocalDateTime requestedAt;
}
