package io.github.jihyundev.spring_batch_etl.batch.etl.processor;

import io.github.jihyundev.spring_batch_etl.api.dto.member.ApiMallMemberDto;
import io.github.jihyundev.spring_batch_etl.batch.exception.InvalidDataException;
import io.github.jihyundev.spring_batch_etl.domain.mall.MallConfig;
import io.github.jihyundev.spring_batch_etl.domain.member.MallMember;
import io.github.jihyundev.spring_batch_etl.domain.member.MemberStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Slf4j
public class MemberItemProcessor implements ItemProcessor<ApiMallMemberDto, MallMember> {
    // 필요하면 ISO_LOCAL_DATE_TIME 쓰고, 포맷이 다르면 패턴 맞춰서 수정
    private static final DateTimeFormatter JOINED_AT_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private final MallConfig mallConfig;

    public MemberItemProcessor(MallConfig mallConfig) {
        this.mallConfig = mallConfig;
    }

    @Override
    public MallMember process(ApiMallMemberDto item) {

        String email = normalizeEmail(item.getEmail());
        String phone = normalizePhone(item.getPhone());
        if (item.getMemberId() == null || item.getMemberId().isBlank()) {
            throw new InvalidDataException("memberId is null or blank. raw=" + item);
        }

        if(email == null){
            throw new InvalidDataException("invalid email is null. memberId=" + item.getMemberId() + ", email=" + email);
        }

        if(phone == null){
            throw new InvalidDataException("invalid phone is null. memberId=" + item.getMemberId() + ", phone=" + phone);
        }

        LocalDateTime joinedAt = parseJoinedAt(item.getCreatedDate());
        MallMember member = MallMember.builder()
                .mallId(mallConfig.getMallId())
                .memberId(item.getMemberId())
                .name(item.getName())
                .phone(phone)
                .email(email)
                .status(MemberStatus.ACTIVE)
                .joinPath(item.getJoinPath())
                .joinedAt(joinedAt)
                .createdAt(LocalDateTime.now())
                .lastModifiedAt(LocalDateTime.now())
                .build();

        log.debug("[MemberItemProcessor] processed member. mallId={}, memberId={}, email={}",
                mallConfig.getMallId(), member.getMemberId(), member.getEmail());

        return member;
    }

    private String normalizePhone(String phone) {
        if(phone == null || phone.isEmpty() || phone.isBlank()) return null;
        String digits = phone.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? null : digits;
    }

    private String normalizeEmail(String email) {
        if(email == null || email.isEmpty() || email.isBlank()) return null;
        String tremmed = email.trim();
        return tremmed.isEmpty() ? null : tremmed.toLowerCase();
    }

    /**
     * 가입일 파싱
     * @param joinedAt
     * @return
     */
    private LocalDateTime parseJoinedAt(String joinedAt) {
        if (joinedAt == null || joinedAt.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(joinedAt, JOINED_AT_FORMATTER);
        } catch (DateTimeParseException e) {
            log.warn("[MemberItemProcessor] failed to parse joinedAt. value={}", joinedAt, e);
            return null;
        }
    }

}
