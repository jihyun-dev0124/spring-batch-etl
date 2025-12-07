package io.github.jihyundev.spring_batch_etl.mapper.mall;

import io.github.jihyundev.spring_batch_etl.domain.batch.BatchErrorLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BatchErrorLogMapper {
    void insertErrorLog(BatchErrorLog log);
    List<BatchErrorLog> findAll();
}
