package io.github.jihyundev.spring_batch_etl.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jihyundev.spring_batch_etl.api.dto.member.ApiMallMemberDto;
import io.github.jihyundev.spring_batch_etl.api.dto.member.ApiMallMembersResponse;
import io.github.jihyundev.spring_batch_etl.api.dto.member.ApiMallMemberCountResponse;
import io.github.jihyundev.spring_batch_etl.api.dto.sales.ApiSalesDto;
import io.github.jihyundev.spring_batch_etl.api.dto.sales.ApiSalesResponse;
import io.github.jihyundev.spring_batch_etl.domain.mall.MallConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@Profile("demo")
@RequiredArgsConstructor
public class FakeMallApiClientImpl implements MallApiClient {

    private final ObjectMapper objectMapper;

    @Override
    public int getMemberCount(MallConfig mallConfig, LocalDateTime startDate, LocalDateTime endDate) {
        ApiMallMemberCountResponse response = readFixture("fixtures/members-count.json", ApiMallMemberCountResponse.class);
        return response.getCount();
    }

    @Override
    public List<ApiMallMemberDto> getMembers(MallConfig mallConfig, int offset, int limit, LocalDateTime startDate, LocalDateTime endDate) {
        String path = resolveMembersFixturePath(offset);
        ApiMallMembersResponse response = readFixture(path, ApiMallMembersResponse.class);
        return response.getMembers();
    }

    @Override
    public List<ApiSalesDto> getSales(MallConfig mallConfig, int offset, int limit, LocalDateTime startDate, LocalDateTime endDate) {
        ApiSalesResponse response = readFixture("fixtures/sales-hourly-2025-11-30.json", ApiSalesResponse.class);
        return response.getSalesList();
    }

    private String resolveMembersFixturePath(int offset) {
        // 예: 0 ~ 999 -> page-0, 1000 ~ 1999 -> page-1...
        int page = offset / 1000;
        return "fixtures/members-page-" + page + ".json";
    }

    private <T> T readFixture(String path, Class<T> type) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalArgumentException("Fixture not found: " + path);
            }
            return objectMapper.readValue(is, type);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read fixture: " + path, e);
        }
    }
}
