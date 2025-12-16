package io.github.jihyundev.spring_batch_etl.mapper.batch;

import io.github.jihyundev.spring_batch_etl.domain.batch.BatchExecutionSummary;
import io.github.jihyundev.spring_batch_etl.dto.request.BatchExecutionCondition;
import io.github.jihyundev.spring_batch_etl.dto.response.BatchExecutionKpi;
import io.github.jihyundev.spring_batch_etl.dto.response.BatchExecutionSummaryDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface BatchExecutionSummaryMapper {
    void insertBatchExecutionSummary(BatchExecutionSummary batchExecutionSummary);

    List<BatchExecutionSummaryDto> selectExecutionSummaries(BatchExecutionCondition condition);

    int countSummaries(BatchExecutionCondition condition);

    BatchExecutionKpi selectTodayKpi(@Param("date") LocalDate date);
}
