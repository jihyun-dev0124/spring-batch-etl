package io.github.jihyundev.spring_batch_etl.dto.request;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class BatchExecutionCondition {
    private Long mallId;
    private String domainType;
    private LocalDate fromDate; // 수집 시작일
    private LocalDate toDate;   // 수집 종료일
    private String status; //COMPLETED, FAILED, STOPPED

    private int page = 0;
    private int size = 20;

    public int offset() {
        return Math.max(page, 0) * Math.max(size, 1);
    }
}
