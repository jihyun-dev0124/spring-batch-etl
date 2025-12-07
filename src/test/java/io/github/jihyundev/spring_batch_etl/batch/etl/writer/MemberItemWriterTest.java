package io.github.jihyundev.spring_batch_etl.batch.etl.writer;

import io.github.jihyundev.spring_batch_etl.domain.member.MallMember;
import io.github.jihyundev.spring_batch_etl.domain.member.MemberStatus;
import io.github.jihyundev.spring_batch_etl.mapper.mall.MallMemberMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberItemWriterTest {

    @Autowired
    MallMemberMapper mallMemberMapper;

    @Test
    void upsert_정상동작_테스트() throws Exception {
        MallMember member1 = MallMember.builder()
                .mallId(1L)
                .memberId("EXT-001")
                .name("김회원")
                .phone("01012345678")
                .email("test@test.com")
                .status(MemberStatus.ACTIVE)
                .joinPath("P")
                .joinedAt(LocalDateTime.now().minusDays(1))
                .createdAt(LocalDateTime.now())
                .lastModifiedAt(LocalDateTime.now())
                .build();

        mallMemberMapper.upsertMallMembers(List.of(member1));

        //DB 조회해서 값 확인
        MallMember findMember1 = mallMemberMapper.findByMallIdAndMemberId(1L, "EXT-001");
        assertNotNull(findMember1);
        assertEquals("김회원", findMember1.getName());

        MallMember member2 = MallMember.builder()
                .mallId(1L)
                .memberId("EXT-002")
                .name("김철수")
                .phone("01011112222")
                .email("test@example.com")
                .status(MemberStatus.ACTIVE)
                .joinPath("P")
                .joinedAt(LocalDateTime.now().minusDays(1))
                .createdAt(LocalDateTime.now())
                .lastModifiedAt(LocalDateTime.now())
                .build();

        mallMemberMapper.upsertMallMembers(List.of(member2));

        MallMember findMember2 = mallMemberMapper.findByMallIdAndMemberId(1L, "EXT-002");
        assertNotNull(findMember2);
        assertEquals("김철수", findMember2.getName());
    }

}