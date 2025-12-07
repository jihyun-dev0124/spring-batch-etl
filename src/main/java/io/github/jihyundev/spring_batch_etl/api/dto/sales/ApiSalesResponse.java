package io.github.jihyundev.spring_batch_etl.api.dto.sales;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ApiSalesResponse {
    @JsonProperty("hourlysales")
    private List<ApiSalesDto> salesList;
}
