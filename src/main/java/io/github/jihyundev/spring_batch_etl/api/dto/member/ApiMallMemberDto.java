package io.github.jihyundev.spring_batch_etl.api.dto.member;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ApiMallMemberDto {
    @JsonProperty("member_id")
    private String memberId;
    private String name;
    private String phone;
    private String email;
    private String birthday;

    @JsonProperty("join_path")
    private String joinPath;

    @JsonProperty("created_date")
    private String createdDate;

    @JsonProperty("last_login_date")
    private String lastLoginDate;
}
