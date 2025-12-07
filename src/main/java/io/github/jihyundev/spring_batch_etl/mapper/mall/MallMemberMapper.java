package io.github.jihyundev.spring_batch_etl.mapper.mall;

import io.github.jihyundev.spring_batch_etl.domain.member.MallMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MallMemberMapper {
    void upsertMallMember(MallMember mallMember);
    void upsertMallMembers(@Param("members") List<MallMember> mallMembers);
    MallMember findByMallIdAndMemberId(@Param("mallId") long mallId, @Param("memberId") String memberId);

    List<MallMember> findAll();
}
