package io.github.jihyundev.spring_batch_etl.mapper.batch;

import io.github.jihyundev.spring_batch_etl.domain.batch.BatchErrorLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BatchErrorLogMapper {
    void insertErrorLog(BatchErrorLog log);
    List<BatchErrorLog> findAll();

    int countByExecutionContext(@Param("jobName") String jobName, @Param("domainType") String domainType, @Param("mallId") Long mallId,
                                @Param("fromDate") LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate);

    int countPendingRetry();
}
