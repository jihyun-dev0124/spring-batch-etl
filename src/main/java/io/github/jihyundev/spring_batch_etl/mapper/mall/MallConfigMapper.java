package io.github.jihyundev.spring_batch_etl.mapper.mall;

import io.github.jihyundev.spring_batch_etl.domain.mall.MallConfig;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MallConfigMapper {
    MallConfig findById(Long mallId);
    List<Long> findAllMallIds();

    void insert(MallConfig mallConfig);
}
