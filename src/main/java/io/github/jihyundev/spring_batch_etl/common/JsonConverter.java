package io.github.jihyundev.spring_batch_etl.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JsonConverter {
    private final ObjectMapper objectMapper;
    public String convertItemToJson(Object item) {
        if(item == null) return null;

        try {
            return objectMapper.writeValueAsString(item);
        } catch (JsonProcessingException e) {
            log.error("[JsonConverter] JSON 직렬화 실패, item ={}", item, e);
            return "{\"error\":\"json_convert_failed\"}";
        }
    }

}
