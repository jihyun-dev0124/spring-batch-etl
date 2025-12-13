package io.github.jihyundev.spring_batch_etl.mapper.batch;

import io.github.jihyundev.spring_batch_etl.domain.batch.BatchExecutionSummary;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BatchExecutionSummaryMapper {
    void insertBatchExecutionSummary(BatchExecutionSummary batchExecutionSummary);
}
