package io.github.jihyundev.spring_batch_etl.api.dto.member;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@NoArgsConstructor
@ToString(callSuper = true)
public class ApiMallMembersResponse {
    @JsonProperty("customersprivacy")
    private List<ApiMallMemberDto> members;
}
