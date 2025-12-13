package io.github.jihyundev.spring_batch_etl.batch.etl.writer;

import io.github.jihyundev.spring_batch_etl.domain.member.MallMember;
import io.github.jihyundev.spring_batch_etl.mapper.mall.MallMemberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class MemberItemWriter implements ItemWriter<MallMember> {
    private final MallMemberMapper mallMemberMapper;

    @Override
    public void write(Chunk<? extends MallMember> chunk) throws Exception {
        if(chunk == null || chunk.isEmpty()) return;

        List<MallMember> items = new ArrayList<>(chunk.getItems());

        //deadlock 방지 -> unique key sort
        items.sort(Comparator.comparing(MallMember::getMallId)
                .thenComparing(MallMember::getMemberId));

        log.debug("[MemberItemWriter] write size={}", items.size());

        // List로 mapper에 전달
        mallMemberMapper.upsertMallMembers(items);
    }
}
