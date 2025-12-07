package io.github.jihyundev.spring_batch_etl.domain.member;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MallMember {
    private Long id;
    private Long mallId;
    private String memberId;
    private String name;
    private String phone;
    private String email;
    private MemberStatus status;
    private String joinPath; //가입경로
    private LocalDateTime joinedAt; //회원가입일

    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;
}
