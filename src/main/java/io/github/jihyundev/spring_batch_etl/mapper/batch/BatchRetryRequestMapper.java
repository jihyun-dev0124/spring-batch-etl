package io.github.jihyundev.spring_batch_etl.mapper.batch;

import io.github.jihyundev.spring_batch_etl.domain.batch.BatchDomainType;
import io.github.jihyundev.spring_batch_etl.domain.batch.BatchRetryRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BatchRetryRequestMapper {
    void upsertFailedBatchRetryRequest(BatchRetryRequest batchRetryRequest);

    void updateDoneBatchRetryRequest(BatchRetryRequest batchRetryRequest);

    List<BatchRetryRequest> findAll();

    BatchRetryRequest findByJobExecutionId(@Param("jobExecutionId") Long jobExecutionId);

}
