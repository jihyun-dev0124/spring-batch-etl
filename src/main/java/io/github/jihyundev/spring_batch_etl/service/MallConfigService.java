package io.github.jihyundev.spring_batch_etl.service;

import io.github.jihyundev.spring_batch_etl.domain.mall.MallConfig;
import io.github.jihyundev.spring_batch_etl.dto.request.BatchExecutionCondition;
import io.github.jihyundev.spring_batch_etl.dto.response.BatchExecutionSummaryDto;
import io.github.jihyundev.spring_batch_etl.mapper.mall.MallConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MallConfigService {
    private final MallConfigMapper mallConfigMapper;

    public MallConfig getMallConfig(Long mallId) {
        MallConfig mallConfig = mallConfigMapper.findById(mallId);
        if(mallConfig == null) {
            throw new IllegalArgumentException("mallId="+mallId+"에 해당하는 MallConfig가 없습니다.");
        }
        return mallConfig;
    }




}
