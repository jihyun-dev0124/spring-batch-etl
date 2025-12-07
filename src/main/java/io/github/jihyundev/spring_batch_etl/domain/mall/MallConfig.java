package io.github.jihyundev.spring_batch_etl.domain.mall;

import lombok.*;

import java.time.LocalDateTime;

@Data
@ToString
@NoArgsConstructor
public class MallConfig {
    private Long mallId;
    private String mallName;
    private String apiUrl;
    private String apiId;
    private String apiPw;
    private String tokenValue;
    private String refreshTokenValue;
    private String tokenExpire;
    private String apiVersion;
    private String isUsable;
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;
}
