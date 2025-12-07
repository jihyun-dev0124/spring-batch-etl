package io.github.jihyundev.spring_batch_etl.api;

import io.github.jihyundev.spring_batch_etl.api.dto.member.ApiMallMemberDto;
import io.github.jihyundev.spring_batch_etl.api.dto.sales.ApiSalesDto;
import io.github.jihyundev.spring_batch_etl.domain.mall.MallConfig;

import java.time.LocalDateTime;
import java.util.List;

public interface MallApiClient {
    int getMemberCount(MallConfig mallConfig, LocalDateTime startDate, LocalDateTime endDate);
    List<ApiMallMemberDto> getMembers(MallConfig mallConfig, int offset, int limit, LocalDateTime startDate, LocalDateTime endDate);
    List<ApiSalesDto> getSales(MallConfig mallConfig, int offset, int limit, LocalDateTime startDate, LocalDateTime endDate);
}
