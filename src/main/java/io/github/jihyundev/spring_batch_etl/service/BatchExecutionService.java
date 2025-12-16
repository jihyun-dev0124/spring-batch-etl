package io.github.jihyundev.spring_batch_etl.service;

import io.github.jihyundev.spring_batch_etl.domain.batch.BatchRetryRequest;
import io.github.jihyundev.spring_batch_etl.dto.request.BatchExecutionCondition;
import io.github.jihyundev.spring_batch_etl.dto.response.BatchExecutionKpi;
import io.github.jihyundev.spring_batch_etl.dto.response.BatchExecutionSummaryDto;
import io.github.jihyundev.spring_batch_etl.mapper.batch.BatchErrorLogMapper;
import io.github.jihyundev.spring_batch_etl.mapper.batch.BatchExecutionSummaryMapper;
import io.github.jihyundev.spring_batch_etl.mapper.batch.BatchRetryRequestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BatchExecutionService {
    private final BatchExecutionSummaryMapper batchExecutionSummaryMapper;
    private final BatchRetryRequestMapper batchRetryRequestMapper;
    private final BatchErrorLogMapper batchErrorLogMapper;

    public int countSummaries(BatchExecutionCondition condition) {
        return batchExecutionSummaryMapper.countSummaries(condition);
    }

    public List<BatchExecutionSummaryDto> findSummaries(BatchExecutionCondition condition) {
        List<BatchExecutionSummaryDto> summaries = batchExecutionSummaryMapper.selectExecutionSummaries(condition);
        for (BatchExecutionSummaryDto summary : summaries) {
            String status = summary.getStatus();
            if(status.equals("FAILED")) {
                BatchRetryRequest batchRetryRequest = batchRetryRequestMapper.findByJobExecutionId(summary.getJobExecutionId());
                if(batchRetryRequest != null) summary.setRetryFlag(1);
            }
        }

        return summaries;
    }

    public BatchExecutionKpi getKpi(BatchExecutionCondition condition) {
        LocalDate today = LocalDate.now();
        BatchExecutionKpi batchExecutionKpi = batchExecutionSummaryMapper.selectTodayKpi(today);
        batchExecutionKpi.setPendingRetryRows(batchErrorLogMapper.countPendingRetry());
        return batchExecutionKpi;
    }
}
