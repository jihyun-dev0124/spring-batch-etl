package io.github.jihyundev.spring_batch_etl.batch.etl.processor;

import io.github.jihyundev.spring_batch_etl.api.dto.member.ApiMallMemberDto;
import io.github.jihyundev.spring_batch_etl.batch.exception.InvalidDataException;
import io.github.jihyundev.spring_batch_etl.domain.mall.MallConfig;
import io.github.jihyundev.spring_batch_etl.domain.member.MallMember;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MemberItemProcessorTest {
    private MemberItemProcessor processor;

    @BeforeEach
    void setUp() {
        MallConfig mallConfig = new MallConfig();
        mallConfig.setMallId(1L);
        processor = new MemberItemProcessor(mallConfig);
    }

    @Test
    void 정상_회원_매핑_테스트() throws Exception {
        ApiMallMemberDto memberDto = ApiMallMemberDto
                .builder()
                .memberId("EXT-001")
                .name("김회원")
                .phone("010-1234-5678")
                .email(" test@test.com ")
                .birthday("1999-06-20")
                .joinPath("P")
                .createdDate("2025-12-02T11:19:27+09:00")
                .lastLoginDate("2025-12-02T11:19:27+09:00")
                .build();

        MallMember result = processor.process(memberDto);

        assertNotNull(result);
        assertEquals(1L, result.getMallId());
        assertEquals("EXT-001", result.getMemberId());
        assertEquals("김회원", result.getName());
        assertEquals("01012345678", result.getPhone()); //핸드폰 번호 숫자만 return
        assertEquals("test@test.com", result.getEmail()); //이메일 공백 제거
        assertNotNull(result.getJoinedAt());
    }

    @Test
    void 실패_회원_매핑_테스트() throws Exception {
        // given
        ApiMallMemberDto memberDto = ApiMallMemberDto
                .builder()
                .memberId("EXT-001")
                .name("김회원")
                .phone("")
                .email(" test@test.com ")
                .birthday("1999-06-20")
                .joinPath("P")
                .createdDate("2025-12-02T11:19:27+09:00")
                .lastLoginDate("2025-12-02T11:19:27+09:00")
                .build();

        // when & then
        InvalidDataException ex = assertThrows(InvalidDataException.class, () -> processor.process(memberDto));

        // 메시지 검증
        assertTrue(ex.getMessage().contains("memberId=EXT-001"));
        assertTrue(ex.getMessage().contains("phone=null"));
    }

}